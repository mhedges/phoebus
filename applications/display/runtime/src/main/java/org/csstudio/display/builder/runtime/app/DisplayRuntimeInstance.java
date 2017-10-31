/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.persist.ModelLoader;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.jobs.JobManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** PV Table Application
 *  @author Kay Kasemir
 */
public class DisplayRuntimeInstance implements AppInstance
{
    // TODO This is ~ RCP RuntimeViewPart
    final private AppDescriptor app;
    final private BorderPane layout = new BorderPane();
    final private DockItemWithInput dock_item;
    private Node toolbar;

    DisplayRuntimeInstance(final AppDescriptor app)
    {
        this.app = app;
        dock_item = createComponents();
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    private DockItemWithInput createComponents()
    {
        toolbar = createToolbar();
        BorderPane.setMargin(toolbar, new Insets(5, 5, 0, 5));
        layout.setTop(toolbar);
        final DockItemWithInput dock_item = new DockItemWithInput(this, layout, null, null);
        DockPane.getActiveDockPane().addTab(dock_item);

        dock_item.addClosedNotification(this::stop);
        return dock_item;
    }

    private Node createToolbar()
    {
        final Separator sep = new Separator();
        HBox.setHgrow(sep, Priority.ALWAYS);
        return new HBox(5,
                        new Label("TODO: TOOLBAR"),
                        sep,
                        new Button("Back"),
                        new Button("Fore")
                        );
    }

    public void raise()
    {
        dock_item.select();
    }

    public void loadDisplayFile(final DisplayInfo info)
    {
        // Set input ASAP so that other requests to open this
        // resource will find this instance and not start
        // another instance
        dock_item.setInput(info.toURL());

        // Load files in background job
        JobManager.schedule("Load Display", monitor ->
        {
            monitor.beginTask(info.getName());
            try
            {
                String parent_display = null;
                ModelLoader.resolveAndLoadModel(parent_display , info.getPath());


                Platform.runLater(() ->
                {
                    layout.setCenter(new Label("TODO: Represent " + info.toString()));
                });
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error loading " + info, ex);
                showError("Error loading " + info);
            }
        });
    }

    private void showError(final String message)
    {
        Platform.runLater(() ->
        {
            // TODO Show error in dock_item
            // See RuntimeViewPart::onDispose()
        });
    }

    public void stop()
    {
        // TODO Stop runtime, dispose representation, release model
    }
}
