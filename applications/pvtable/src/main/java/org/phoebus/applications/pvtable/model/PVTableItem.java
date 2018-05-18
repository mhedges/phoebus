/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

import static org.phoebus.applications.pvtable.PVTableApplication.logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.phoebus.applications.pvtable.Settings;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVListener;
import org.phoebus.pv.PVPool;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.VByteArray;
import org.phoebus.vtype.VEnum;
import org.phoebus.vtype.VEnumArray;
import org.phoebus.vtype.VNumber;
import org.phoebus.vtype.VNumberArray;
import org.phoebus.vtype.VString;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.Disposable;

/** One item (row) in the PV table.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTableItem
{
    volatile PVTableItemListener listener;

    private volatile boolean selected = true;

    /** Primary PV name */
    private String name = null;

    /** Last known value of the PV */
    private volatile VType value;

    /** Value of the PV's description */
    private volatile String desc_value = "";

    /** Saved (snapshot) value */
    private volatile Optional<SavedValue> saved = Optional.empty();

    /** TimeStamp Saved */
    private volatile String time_saved = "";

    /** Does current value differ from saved value? */
    private volatile boolean has_changed;

    /** Tolerance for comparing current and saved (if they're numeric) */
    private double tolerance;

    private volatile boolean use_completion = true;
    
    final private AtomicBoolean isReadonly = new AtomicBoolean();

    /** Primary PV */
    final private AtomicReference<PV> pv = new AtomicReference<PV>(null);
    
    final private AtomicReference<Disposable> value_flow = new AtomicReference<Disposable>(null);
    final private AtomicReference<Disposable> access_flow = new AtomicReference<Disposable>(null);
    final private AtomicReference<Disposable> connection_flow = new AtomicReference<Disposable>(null);

    /** Description PV */
    final private AtomicReference<PV> desc_pv = new AtomicReference<PV>(null);
    
    final private AtomicReference<Disposable> desc_value_flow = new AtomicReference<Disposable>(null);
    final private AtomicReference<Disposable> desc_connection_flow = new AtomicReference<Disposable>(null);

    /** Initialize
     *
     *  @param name
     *  @param tolerance
     *  @param saved
     *  @param listener
     */
    PVTableItem(final String name, final double tolerance, final PVTableItemListener listener)
    {
        this(name, tolerance, null, "", listener);
    }

    /** Initialize
     *
     *  @param name
     *  @param tolerance
     *  @param saved
     *  @param listener
     */
    PVTableItem(final String name, final double tolerance,
                final SavedValue saved,
                final String time_saved,
                final PVTableItemListener listener)
    {
        this.listener = listener;
        this.tolerance = tolerance;
        this.saved = Optional.ofNullable(saved);
        this.time_saved = time_saved;
        createPVs(name);
        determineIfChanged();
    }

    /** Set PV name and create reader/writer
     *
     *  @param name Primary PV name
     */
    private void createPVs(final String name)
    {
        this.name = name;
        // Ignore empty PVs or comments
        if (name.isEmpty() || isComment())
        {
            updateValue(null);
            return;
        }
        try
        {
            updateValue(ValueFactory.newVString("",
                        ValueFactory.newAlarm(AlarmSeverity.UNDEFINED, "Not connected"),
                        ValueFactory.timeNow()));
            final PV new_pv = PVPool.getPV(name);
            final Disposable new_value_flow = new_pv.onValueEvent(BackpressureStrategy.LATEST).subscribe(value->{
            	updateValue(value);
            });
            final Disposable new_access_flow = new_pv.onAccessRightsEvent(BackpressureStrategy.LATEST).subscribe(readonly->{
            	isReadonly.set(readonly); 
            	listener.tableItemChanged(PVTableItem.this);
            });
            final Disposable new_conection_flow = new_pv.onConnectionEvent(BackpressureStrategy.LATEST).subscribe(connection->{
            	if(connection==false) {
            		updateValue(ValueFactory.newVString(
                            "Disconnected", ValueFactory
                                    .newAlarm(AlarmSeverity.UNDEFINED, "Disconnected"),
                            ValueFactory.timeNow()));
            	}
            });
            pv.set(new_pv);
            value_flow.set(new_value_flow);
            access_flow.set(new_access_flow);
            connection_flow.set(new_conection_flow);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create PV " + name, ex);
            updateValue(ValueFactory.newVString("PV Error",
                        ValueFactory.newAlarm(AlarmSeverity.UNDEFINED, "No PV"),
                        ValueFactory.timeNow()));
        }

        if (Settings.show_description)
        {
            // Determine DESC field.
            // If name already includes a field,
            // replace it with DESC field.
            final int sep = name.lastIndexOf('.');
            final String desc_name = sep >= 0
                    ? name.substring(0, sep) + ".DESC"
                    : name + ".DESC";
            try
            {
                final PV new_desc_pv = PVPool.getPV(desc_name);
                final Disposable new_desc_value_flow = new_desc_pv.onValueEvent(BackpressureStrategy.LATEST).subscribe(value->{
                	if (value instanceof VString)
                    {
                        desc_value = ((VString) value).getValue();
                    }
                    else
                    {
                        desc_value = "";
                    }
                    listener.tableItemChanged(PVTableItem.this);
                });
                final Disposable new_desc_connection_flow = new_desc_pv.onConnectionEvent(BackpressureStrategy.LATEST).subscribe(connection->{
                	if(connection==false) {
                		desc_value = "";
                	}
                });
                desc_pv.set(new_desc_pv);
                desc_value_flow.set(new_desc_value_flow);
                desc_connection_flow.set(new_desc_connection_flow);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Skipping " + desc_name);
            }
        }
    }

    /** @return <code>true</code> if item is selected to be restored */
    public boolean isSelected()
    {
        return selected && !isComment();
    }

    /** @param selected Should item be selected to be restored? */
    public void setSelected(final boolean selected)
    {
        boolean was_selected = this.selected;
        this.selected = selected && !isComment();
        if (was_selected != this.selected)
            listener.tableItemSelectionChanged(this);
    }

    /** @return Returns the name of the 'main' PV. */
    public String getName()
    {
        return name;
    }

    /** @return Returns the comment. */
    public String getComment()
    {
        // Skip initial "#". Trim in case of another space from "# "
        return name.substring(1).trim();
    }

    /** Update PV name
     *
     *  <p>Also resets saved and current value,
     *  since it no longer applies to the new name.
     *
     *  @param new_name PV Name
     *  @return <code>true</code> if name was indeed changed
     */
    public boolean updateName(final String new_name)
    {
        if (name.equals(new_name))
            return false;
        dispose();
        saved = Optional.empty();
        time_saved = "";
        value = null;
        has_changed = false;
        createPVs(new_name);
        return true;
    }

    /** @param new_value New value of item */
    protected void updateValue(final VType new_value)
    {
        value = new_value;
        determineIfChanged();
        listener.tableItemChanged(this);
    }

    /** @return Value */
    public VType getValue()
    {
        return value;
    }

    /** @return Description */
    public String getDescription()
    {
        return desc_value;
    }

    /** @return Enum options for current value, <code>null</code> if not enumerated */
    public String[] getValueOptions()
    {
        final VType copy = value;
        if (!(copy instanceof VEnum))
            return null;
        final List<String> options = ((VEnum) copy).getLabels();
        return options.toArray(new String[options.size()]);
    }

    /** @return <code>true</code> when PV is writable */
    public boolean isWritable()
    {
        final PV the_pv = pv.get();
        return the_pv != null && isReadonly.get() == false && !isComment();
    }

    /** @return Await completion when restoring value to PV? */
    public boolean isUsingCompletion()
    {
        return use_completion;
    }

    /** @param use_completion Await completion when restoring value to PV? */
    public void setUseCompletion(final boolean use_completion)
    {
        this.use_completion = use_completion;
    }

    /** @param new_value Value to write to the item's PV */
    public void setValue(String new_value)
    {
        new_value = new_value.trim();
        try
        {
            final PV the_pv = pv.get();
            if (the_pv == null)
                throw new Exception("Not connected");

            final VType pv_type = the_pv.onSingleValueEvent().blockingGet();
            if (pv_type instanceof VNumber)
            {
                if (Settings.show_units)
                {   // Strip units so that only the number gets written
                    final String units = ((VNumber)pv_type).getUnits();
                    if (units.length() > 0  &&  new_value.endsWith(units))
                        new_value = new_value.substring(0, new_value.length() - units.length()).trim();
                }
                the_pv.setValue(Double.parseDouble(new_value));
            }
            else if (pv_type instanceof VEnum)
            { // Value is displayed as "6 =
              // 1 second"
                // Locate the initial index, ignore following text
                final int end = new_value.indexOf(' ');
                final int index = end > 0
                        ? Integer.valueOf(new_value.substring(0, end))
                        : Integer.valueOf(new_value);
                the_pv.setValue(index);
            }
            else if (pv_type instanceof VByteArray && Settings.treat_byte_array_as_string)
            {
                // Write string as byte array WITH '\0' TERMINATION!
                final byte[] bytes = new byte[new_value.length() + 1];
                System.arraycopy(new_value.getBytes(), 0, bytes, 0,
                        new_value.length());
                bytes[new_value.length()] = '\0';
                the_pv.setValue(bytes);
            }
            else if (pv_type instanceof VNumberArray)
            {
                final String[] elements = new_value.split("\\s*,\\s*");
                final int N = elements.length;
                final double[] data = new double[N];
                for (int i = 0; i < N; ++i)
                    data[i] = Double.parseDouble(elements[i]);
                the_pv.setValue(data);
            }
            else if (pv_type instanceof VEnumArray)
            {
                final String[] elements = new_value.split("\\s*,\\s*");
                final int N = elements.length;
                final int[] data = new int[N];
                for (int i = 0; i < N; ++i)
                    data[i] = (int) Double.parseDouble(elements[i]);
                the_pv.setValue(data);
            }
            else // Write other types as string
                the_pv.setValue(new_value);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot set " + getName() + " = " + new_value, ex);
        }
    }

    /** Save current value as saved value, w/ timestamp */
    public void save()
    {
        if (isComment())
            return;

        try
        {
            if (value == null  ||  VTypeHelper.getSeverity(value).compareTo(AlarmSeverity.INVALID) >= 0)
            {   // Nothing to save
                time_saved = "";
                saved = Optional.empty();
            }
            else
            {
                time_saved = TimestampHelper.format(VTypeHelper.getTimestamp(value));
                saved = Optional.of(SavedValue.forCurrentValue(value));
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot save value of " + getName(), ex);
        }
        determineIfChanged();
    }

    /** @return time_saved, the timestamp saved */
    public String getTime_saved()
    {
        return time_saved;
    }

    /** @param time_saved, the current value of timestamp */
    public void setTime_saved(String time_saved)
    {
        this.time_saved = time_saved;
    }

    /** Write saved value back to PV
     *  @param completion_timeout_seconds
     *  @throws Exception on error
     */
    public void restore(final long completion_timeout_seconds) throws Exception
    {
        if (isComment() || !isWritable())
            return;

        final PV the_pv = pv.get();
        final SavedValue the_value = saved.orElse(null);
        if (the_pv == null || the_value == null)
            return;

        the_value.restore(the_pv, isUsingCompletion() ? completion_timeout_seconds : 0);
    }

    /** @return Returns the saved_value */
    public Optional<SavedValue> getSavedValue()
    {
        return saved;
    }

    /** @return Tolerance for comparing saved and current value */
    public double getTolerance()
    {
        return tolerance;
    }

    /** @param tolerance Tolerance for comparing saved and current value */
    public void setTolerance(final double tolerance)
    {
        this.tolerance = tolerance;
        determineIfChanged();
        listener.tableItemChanged(this);
    }

    /** @return <code>true</code> if this item is a comment instead of a PV with
     *          name, value etc.
     */
    public boolean isComment()
    {
        return name.startsWith("#");
    }

    /** @return <code>true</code> if value has changed from saved value */
    public boolean isChanged()
    {
        return has_changed;
    }

    /** Update <code>has_changed</code> based on current and saved value */
    private void determineIfChanged()
    {
        if (isComment())
        {
            has_changed = false;
            return;
        }
        final Optional<SavedValue> saved_value = saved;
        if (!saved_value.isPresent())
        {
            has_changed = false;
            return;
        }
        try
        {
            has_changed = !saved_value.get().isEqualTo(value, tolerance);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Change test failed for " + getName(), ex);
        }
    }

    /** Must be called to release resources when item no longer in use */
    public void dispose()
    {
        PV the_pv = pv.getAndSet(null);
        Disposable the_value_flow = value_flow.getAndSet(null);
        Disposable the_access_flow = access_flow.getAndSet(null);
        Disposable the_connection_flow = connection_flow.getAndSet(null);
        if (the_pv != null && the_value_flow !=null && the_access_flow != null && the_connection_flow != null)
        {
            the_value_flow.dispose();
            the_access_flow.dispose();
            the_connection_flow.dispose();
            PVPool.releasePV(the_pv);
        }

        the_pv = desc_pv.getAndSet(null);
        Disposable the_desc_value_flow = desc_value_flow.getAndSet(null);
        Disposable the_desc_connection_flow = desc_connection_flow.getAndSet(null);
        if (the_pv != null)
        {
            the_desc_value_flow.dispose();
            the_desc_connection_flow.dispose();
            PVPool.releasePV(the_pv);
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(name);
        if (!isWritable())
            buf.append(" (read-only)");

        buf.append(" = ").append(VTypeHelper.toString(value));
        final Optional<SavedValue> saved_value = saved;
        if (saved_value.isPresent())
        {
            if (has_changed)
                buf.append(" ( != ");
            else
                buf.append(" ( == ");
            buf.append(saved_value.get().toString()).append(" +- ")
                    .append(tolerance).append(")");
        }
        return buf.toString();
    }
}
