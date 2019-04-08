/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.data.providers.jmasar;

import java.util.List;

import org.phoebus.applications.saveandrestore.data.DataProvider;
import org.phoebus.applications.saveandrestore.data.DataProviderException;

import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Node;
import se.esss.ics.masar.model.SnapshotItem;

public class JMasarDataProvider implements DataProvider {

	private JMasarClient jmasarClient;

	public JMasarDataProvider() {
		jmasarClient = new JMasarClient();
	}

	@Override
	public Node getRootNode() {
		return jmasarClient.getRoot();
	}

	@Override
	public Node getNode(String uniqueNodeId){
		return jmasarClient.getNode(uniqueNodeId);
	}

	@Override
	public List<Node> getChildNodes(Node node) {
		return jmasarClient.getChildNodes(node);
	}

	@Override
	public Node updateNode(Node nodeToUpdate) {
		try {
			return jmasarClient.updateNode(nodeToUpdate);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Node createNode(String parentsUniqueId, Node node) {
		return jmasarClient.createNewNode(parentsUniqueId, node);
	}
	
	@Override
	public String getServiceIdentifier() {
		return "JMasar service (" + jmasarClient.getServiceUrl() + ")";
	}

	@Override
	public boolean deleteNode(String uniqueNodeId) {

		try {
			jmasarClient.deleteNode(uniqueNodeId);
		} catch (DataProviderException e) {
			return false;
		}
		return true;
	}
	
	@Override
	public List<ConfigPv> getConfigPvs(String uniqueNodeId) {
		
		return jmasarClient.getConfigPvs(uniqueNodeId);
	}

    @Override
    public Node getSaveSetForSnapshot(String uniqueNodeId) {

        return jmasarClient.getParentNode(uniqueNodeId);
    }
	
	@Override
	public Node saveSaveSet(String parentsUniqueId, Node config) {
		return jmasarClient.createNewNode(parentsUniqueId, config);
	}
	
	@Override
	public Node updateSaveSet(Node configToUpdate, List<ConfigPv> confgPvList) {
		return jmasarClient.updateConfiguration(configToUpdate, confgPvList);
	}

	@Override
	public String getServiceVersion() {
		return jmasarClient.getJMasarServiceVersion();
	}



	@Override
	public Node takeSnapshot(String uniqueNodeId){
		return jmasarClient.takeSnapshot(uniqueNodeId);
	}

	@Override
	public boolean tagSnapshotAsGolden(String uniqueNodeId){
		try {
			jmasarClient.tagSnapshotAsGolden(uniqueNodeId);
			return true;
		}
		catch(DataProviderException e){
			return false;
		}
	}

	@Override
	public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId){
		return jmasarClient.getSnapshotItems(snapshotUniqueId);
	}

	@Override
	public Node getParentNode(String uniqueNodeId){
		return jmasarClient.getParentNode(uniqueNodeId);
	}

	@Override
	public ConfigPv updateSingleConfigPv(String currentPvName, String newPvName, String currentReadbackPvName, String newReadbackPvName){
		return jmasarClient.updateSingleConfigPv(currentPvName, newPvName, currentReadbackPvName, newReadbackPvName);
	}

	@Override
	public Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment){
		return jmasarClient.saveSnapshot(configUniqueId, snapshotItems, snapshotName, comment);
	}
}