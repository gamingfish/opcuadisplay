package me.steffenjacobs.opcuadisplay.opcInterface.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import com.google.common.collect.Lists;

import me.steffenjacobs.opcuadisplay.Activator;
import me.steffenjacobs.opcuadisplay.management.node.NodeNavigator;
import me.steffenjacobs.opcuadisplay.management.node.NodeNavigator.NodeManipulator;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedBaseNode;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedDataTypeNode;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedMethodNode;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedObjectNode;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedObjectTypeNode;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedReference;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedReferenceTypeNode;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedVariableNode;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedVariableTypeNode;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedViewNode;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.AliasTable;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.ListOfReferences;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.NodeIdAlias;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.Reference;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UADataType;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UAInstance;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UAMethod;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UANode;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UANodeSet;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UAObject;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UAObjectType;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UAReferenceType;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UAType;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UAVariable;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UAVariableType;
import me.steffenjacobs.opcuadisplay.management.node.domain.generated.UAView;

/** @author Steffen Jacobs */
public class XmlExport {

	private static XmlExport instance;

	private static Set<NodeIdAlias> aliases;

	/** constructor creates the basic aliases */
	private XmlExport() {
		aliases = new HashSet<>();
		NodeIdAlias ali = new NodeIdAlias();
		ali.setAlias("Organzies");
		ali.setValue("ns=0;i=35");
		NodeIdAlias ali2 = new NodeIdAlias();
		ali2.setAlias("HasTypeDefinition");
		ali2.setValue("ns=0;i=40");
		NodeIdAlias ali3 = new NodeIdAlias();
		ali3.setAlias("HasSubtype");
		ali3.setValue("ns=0;i=45");
		NodeIdAlias ali4 = new NodeIdAlias();
		ali4.setAlias("HasComponent");
		ali4.setValue("ns=0;i=47");
		NodeIdAlias ali5 = new NodeIdAlias();
		ali5.setAlias("HasProperty");
		ali5.setValue("ns=0;i=46");
		aliases.add(ali);
		aliases.add(ali2);
		aliases.add(ali3);
		aliases.add(ali4);
		aliases.add(ali5);
	}

	public static XmlExport getInstance() {
		if (instance == null) {
			instance = new XmlExport();
		}
		return instance;
	}

	/**
	 * persists the node tree <i>cbn</i> into the file <i>xmlFile</i>
	 * 
	 * @param baseDataTypesImplicit
	 *            true if base data types are implicit and should not be stored in
	 *            the XML file
	 * @param freeOpcUaModelerCompatibility
	 *            true, if compatibility with the free opc ua modeler should be
	 *            ensured
	 */
	public void writeToFile(String xmlFile, CachedBaseNode cbn, boolean baseDataTypesImplicit, boolean freeOpcUaModelerCompatibility, String namespace) {

		UANodeSet nodeSet = new UANodeSet();

		nodeSet.getUAObjectOrUAVariableOrUAMethod().addAll(parseObjectTree(cbn));

		Map<String, UANode> nodesById = new HashMap<>();

		// filter by namespace
		if (namespace != null && !namespace.equals("")) {
			Iterator<UANode> it = nodeSet.getUAObjectOrUAVariableOrUAMethod().iterator();
			while (it.hasNext()) {
				UANode node = it.next();
				try {
					String ns = node.getNodeId().split(";")[0].split("=")[1];
					if (!namespace.equals(ns)) {
						it.remove();
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					// nothing to do
				}
			}
		}

		for (UANode node : nodeSet.getUAObjectOrUAVariableOrUAMethod()) {
			nodesById.put(node.getNodeId(), node);
		}

		List<UANode> nodes = nodeSet.getUAObjectOrUAVariableOrUAMethod();

		// add backward references
		for (UANode node : nodes) {
			for (Reference ref : node.getReferences().getReference()) {
				if (ref.isIsForward()) {
					UANode nodeRef = nodesById.get(ref.getValue());
					if (nodeRef != null) {
						Reference refBw = new Reference();
						refBw.setIsForward(false);
						refBw.setReferenceType(ref.getReferenceType());
						refBw.setValue(node.getNodeId());
						nodeRef.getReferences().getReference().add(refBw);
					}
				}
			}
		}

		if (baseDataTypesImplicit) {
			// load base data types nodeset to remove it from exported node set
			InputStream is = this.getClass().getClassLoader().getResourceAsStream("base.xml");

			try {
				JAXBContext context = JAXBContext.newInstance(UANodeSet.class);
				Unmarshaller um = context.createUnmarshaller();
				UANodeSet nodeSetBase = (UANodeSet) um.unmarshal(new InputStreamReader(is));

				for (UANode n : nodeSetBase.getUAObjectOrUAVariableOrUAMethod()) {
					if (!n.getNodeId().contains("ns=")) {
						n.setNodeId("ns=0;" + n.getNodeId());
					}
				}

				nodeSet.getUAObjectOrUAVariableOrUAMethod().removeAll(nodeSetBase.getUAObjectOrUAVariableOrUAMethod());

			} catch (JAXBException e) {
				e.printStackTrace();
			}
		}

		if (freeOpcUaModelerCompatibility) {

			for (UANode node : nodeSet.getUAObjectOrUAVariableOrUAMethod()) {
				// write 0 into minimum sampling interval if != 0
				if (node instanceof UAVariable) {
					((UAVariable) node).getArrayDimensions().clear();
					((UAVariable) node).getArrayDimensions().add("0");
				}

			}

			// types before instances
			nodeSet.getUAObjectOrUAVariableOrUAMethod().sort(new Comparator<UANode>() {
				@Override
				public int compare(UANode o1, UANode o2) {
					if (o2 instanceof UAType && o1 instanceof UAType || o1 instanceof UAInstance && o2 instanceof UAInstance) {
						return 0;
					} else if (o1 instanceof UAType) {
						return -1;
					} else {
						return 1;
					}
				}
			});

			boolean swapped = false;

			do {
				swapped = false;
				for (int i = 0; i < nodes.size(); i++) {
					UANode node = nodes.get(i);
					if (node instanceof UAInstance) {
						UANode parent = nodesById.get(((UAInstance) node).getParentNodeId());
						int indexParent = nodes.indexOf(parent);
						int indexNode = nodes.indexOf(node);
						if (indexNode < indexParent) {
							// swap nodes
							// indexB = 5, indexA = 2

							// 1 2 A 3 4 B 5 6
							nodes.remove(indexParent);
							// 1 2 A 3 4 5 6
							nodes.remove(indexNode);
							// 1 2 3 4 5 6
							nodes.add(indexNode, parent);
							// 1 2 B 3 4 5 6
							nodes.add(indexParent, node);
							// 1 2 B 3 4 A 5 6
							swapped = true;
						}
					}
					if (swapped) {
						break;
					}
				}
			} while (swapped);
		}

		// add aliases
		if (nodeSet.getAliases() == null) {
			nodeSet.setAliases(new AliasTable());
		}
		nodeSet.getAliases().getAlias().addAll(aliases);

		try {
			// create JAXB context and instantiate marshaller
			JAXBContext context = JAXBContext.newInstance(UANodeSet.class);
			Marshaller m;
			m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			// Write to File
			m.marshal(nodeSet, new File(xmlFile));
		} catch (JAXBException e) {
			Activator.openMessageBoxError("Error exporting XML", "" + e.getLinkedException().getMessage());
			e.printStackTrace();
		}

		try {
			List<String> lines = Files.readAllLines(Paths.get(xmlFile));

			lines.replaceAll(s -> s.replace("<UANodeSet xmlns=\"http://opcfoundation.org/UA/2011/03/UANodeSet.xsd\">", "<UANodeSet xmlns=\"http://opcfoundation.org/UA/2011/03/UANodeSet.xsd\" "
					+ "xmlns:uax=\"http://opcfoundation.org/UA/2008/02/Types.xsd\" " + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"));

			if (freeOpcUaModelerCompatibility) {
				// cast MinimumSamplingInterval to integer
				lines.replaceAll(s -> s.replaceAll("(MinimumSamplingInterval=\".*)\\.[0-9]+(\")", "$1$2"));
			}
			Files.write(Paths.get(xmlFile), lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * converts the value to a tag and adds the associated alias to the aliases set
	 * 
	 * @return the tag associated with the value inside <i>obj</i>
	 */
	private Object convertValue(Object obj, NodeId datatype) {
		if (obj != null) {
			switch (((UInteger) datatype.getIdentifier()).intValue()) {
			case 1:
				aliases.add(new NodeIdAlias("ns=0;i=1", "Boolean"));
				return createTag(obj);
			case 2:
				aliases.add(new NodeIdAlias("ns=0;i=2", "SByte"));
				return createTag(obj);
			case 3:
				aliases.add(new NodeIdAlias("ns=0;i=3", "Byte"));
				return createTag(obj);
			case 4:
				aliases.add(new NodeIdAlias("ns=0;i=4", "Int16"));
				return createTag(obj);
			case 5:
				aliases.add(new NodeIdAlias("ns=0;i=5", "UInt16"));
				return createTag(obj);
			case 6:
				aliases.add(new NodeIdAlias("ns=0;i=6", "Int32"));
				return createTag(obj);
			case 7:
				aliases.add(new NodeIdAlias("ns=0;i=7", "UInt32"));
				return createTag(obj);
			case 8:
				aliases.add(new NodeIdAlias("ns=0;i=8", "Int64"));
				return createTag(obj);
			case 9:
				aliases.add(new NodeIdAlias("ns=0;i=9", "UInt64"));
				return createTag(obj);
			case 10:
				aliases.add(new NodeIdAlias("ns=0;i=10", "Float"));
				return createTag(obj);
			case 11:
				aliases.add(new NodeIdAlias("ns=0;i=11", "Double"));
				return createTag(obj);
			case 12:
				aliases.add(new NodeIdAlias("ns=0;i=12", "String"));
				return createTag(obj);
			case 13:
				aliases.add(new NodeIdAlias("ns=0;i=13", "DateTime"));
				return createTag(obj);
			case 27:
				return convertValue(obj, new NodeId(0, 6));
			case 28:
				return convertValue(obj, new NodeId(0, 7));
			default:
				return null;
			}
		}
		return obj;
	}

	/** @return the Tag associated with <i>obj</i> */
	private JAXBElement<String> createTag(Object obj) {
		String type = obj.getClass().getSimpleName();
		type = type.equals("Long") ? "Int64" : type.equals("Integer") ? "Int32" : type;
		return new JAXBElement<String>(new QName("uax:" + type), String.class, obj.toString());
	}

	/** @return the converted UANode based on the CachedBaseNode <i>cbn</i> */
	private UANode parseNode(CachedBaseNode cbn) {
		UANode uaNode = null;

		if (cbn instanceof CachedReferenceTypeNode) {
			CachedReferenceTypeNode node = (CachedReferenceTypeNode) cbn;
			UAReferenceType urt = new UAReferenceType();

			urt.setIsAbstract(node.isAbstract());
			urt.setSymmetric(node.isSymmetric());

			urt.getInverseName().add(parseLocalizedText(node.getInverseName()));

			uaNode = urt;
		}

		if (cbn instanceof CachedObjectNode) {
			CachedObjectNode node = (CachedObjectNode) cbn;
			UAObject uao = new UAObject();

			// event notifier
			uao.setEventNotifier(node.getEventNotifier() != null ? node.getEventNotifier().shortValue() : 0);

			uao.setParentNodeId(node.getParent() != null && node.getParent().getNodeId() != null ? parseNodeId(node.getParent().getNodeId()) : null);

			uaNode = uao;
		}

		else if (cbn instanceof CachedVariableNode) {
			CachedVariableNode node = (CachedVariableNode) cbn;
			UAVariable uav = new UAVariable();

			UAVariable.Value val = new UAVariable.Value();
			val.setAny(convertValue(node.getValue(), node.getDataType()));
			uav.setValue(val);

			uav.setDataType(parseNodeId(node.getDataType()));
			uav.setValueRank(node.getValueRank());

			uav.getArrayDimensions().addAll(Lists.newArrayList(node.getArrayDimensions()).stream().map(x -> x.toString()).collect(Collectors.toList()));

			uav.setAccessLevel(node.getAccessLevel().shortValue());
			uav.setUserAccessLevel(node.getUserAccessLevel().shortValue());
			uav.setMinimumSamplingInterval(node.getMinimumSamplingInterval());
			uav.setHistorizing(node.isHistorizing());

			uav.setParentNodeId(node.getParent() != null && node.getParent().getNodeId() != null ? parseNodeId(node.getParent().getNodeId()) : null);

			uaNode = uav;
		}

		else if (cbn instanceof CachedMethodNode) {
			CachedMethodNode node = (CachedMethodNode) cbn;
			UAMethod uam = new UAMethod();

			uam.setExecutable(node.isExecutable());
			uam.setUserExecutable(node.isUserExecutable());

			uam.setParentNodeId(node.getParent() != null && node.getParent().getNodeId() != null ? parseNodeId(node.getParent().getNodeId()) : null);

			uaNode = uam;
		}

		else if (cbn instanceof CachedViewNode) {
			CachedViewNode node = (CachedViewNode) cbn;
			UAView uav = new UAView();

			uav.setContainsNoLoops(node.isContainsNoLoop());
			uav.setEventNotifier(node.getEventNotifier().shortValue());

			uaNode = uav;
		}

		else if (cbn instanceof CachedObjectTypeNode) {
			CachedObjectTypeNode node = (CachedObjectTypeNode) cbn;
			UAObjectType uao = new UAObjectType();
			uao.setIsAbstract(node.isAbstract());

			uaNode = uao;
		}

		else if (cbn instanceof CachedDataTypeNode) {
			CachedDataTypeNode node = (CachedDataTypeNode) cbn;
			UADataType uad = new UADataType();
			uad.setIsAbstract(node.isAbstract());

			uaNode = uad;
		}

		else if (cbn instanceof CachedVariableTypeNode) {
			CachedVariableTypeNode node = (CachedVariableTypeNode) cbn;
			UAVariableType uav = new UAVariableType();

			UAVariableType.Value val = new UAVariableType.Value();
			val.setAny(convertValue(node.getValue(), node.getDataType()));
			uav.setValue(val);

			uav.setDataType(parseNodeId(node.getDataType()));
			uav.setValueRank(node.getValueRank());

			uav.getArrayDimensions().addAll(Lists.newArrayList(node.getArrayDimensions()).stream().map(x -> x.toString()).collect(Collectors.toList()));

			uav.setIsAbstract(node.isAbstract());

			uaNode = uav;
		}

		// set node id
		if (uaNode == null) {
			System.out.println(NodeNavigator.getInstance().pathAsString(cbn) + " - " + cbn.getNodeClass());
		}
		uaNode.setNodeId(parseNodeId(cbn.getNodeId()));

		// set browse name
		uaNode.setBrowseName(cbn.getBrowseName().toParseableString());

		// set description
		uaNode.getDescription().add(parseLocalizedText(cbn.getDescription()));

		// set display name
		uaNode.getDisplayName().add(parseLocalizedText(cbn.getDisplayName()));

		// user write mask
		uaNode.setUserWriteMask(cbn.getUserWriteMask().longValue());

		// write mask
		uaNode.setWriteMask(cbn.getWriteMask().longValue());

		// references
		uaNode.setReferences(parseReferences(cbn.getReferences()));

		return uaNode;
	}

	/** @return the converted ListOfReferences based on <i>references</i> */
	private ListOfReferences parseReferences(List<CachedReference> references) {
		ListOfReferences result = new ListOfReferences();

		for (CachedReference ref : references) {
			Reference r = new Reference();
			r.setReferenceType(ref.getReferenceType());
			if (ref.getRefNodeId() == null) {
				System.out.println(ref.getTypeDefinition() + " - " + ref.getReferenceType());
			}
			r.setValue(parseNodeId(ref.getRefNodeId()));
			// only forward references are stored in prototype
			r.setIsForward(true);
			result.getReference().add(r);
		}

		return result;
	}

	/** @return the converted NodeId based on <i>id</i> */
	private String parseNodeId(NodeId id) {
		try {
			Integer.parseInt(id.getIdentifier().toString());
			return "ns=" + id.getNamespaceIndex() + ";i=" + id.getIdentifier().toString();
		} catch (NumberFormatException e) {
			return "ns=" + id.getNamespaceIndex() + ";s=" + id.getIdentifier().toString();
		}
	}

	/**
	 * @return the entire object tree of <i>cbn</i> as a flat list of UANodes.
	 */
	private List<UANode> parseObjectTree(CachedBaseNode cbn) {

		final List<CachedBaseNode> list = new ArrayList<>();

		NodeNavigator.getInstance().iterateNodes(cbn, new NodeManipulator() {

			@Override
			public void manipulate(CachedBaseNode cbn) {
				list.add(cbn);
			}
		});

		return list.stream().map(x -> parseNode(x)).collect(Collectors.toList());
	}

	/** convert the localized texts */
	private me.steffenjacobs.opcuadisplay.management.node.domain.generated.LocalizedText parseLocalizedText(LocalizedText lt) {
		me.steffenjacobs.opcuadisplay.management.node.domain.generated.LocalizedText res = new me.steffenjacobs.opcuadisplay.management.node.domain.generated.LocalizedText();
		res.setLocale(lt.getLocale());
		res.setValue(lt.getText());
		return res;
	}
}
