package encap;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

enum RelTypes implements RelationshipType
{
    OWNS, REFERSTO, POINTSTO
}

public class Neo4jService {
	
	// single instance
	private static Neo4jService instance = null;

	private String DB_PATH = null;
	private GraphDatabaseService graphDb;
	
	public static Neo4jService getInstance() {
		if (instance == null) {
			instance = new Neo4jService();
		}
		return instance;
	}
	
	private Neo4jService() {
		init();
	}

	private void init() {
		Properties props=System.getProperties();
		String userDir = props.getProperty("user.home");
		DB_PATH = userDir + "/Documents/foobar.graphdb";
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
		registerShutdownHook(graphDb);
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
	
	public void store(MethodCG mcg) {
		String mtdSig = CommonUtils.getMethodSignature(mcg.getMtd());
		ConnectionGraph cg = mcg.getExitCG();
		
		try (Transaction tx = graphDb.beginTx()) {
			for (CGNode cgnode : cg.getNodes()) {
				Node neoNode = graphDb.createNode();				
				neoNode.setProperty("method", mtdSig);
				neoNode.setProperty("astnode", cgnode.toNeo4jString());
				neoNode.setProperty("escape", cgnode.getEscapeState().toString());

				if (cgnode instanceof ParamVarNode)
					neoNode.addLabel(DynamicLabel.label("ParamVar"));
				else if (cgnode instanceof LocalVarNode)
					neoNode.addLabel(DynamicLabel.label("LocalVar"));
				else if (cgnode instanceof ObjectNode) {
					neoNode.addLabel(DynamicLabel.label("Object"));
					
					if (cgnode.getASTNode() instanceof TypeDeclaration) {
						Node thisRef = graphDb.createNode();
						thisRef.addLabel(DynamicLabel.label("LocalVar"));				
						thisRef.setProperty("method", mtdSig);
						thisRef.setProperty("astnode", "this");
						thisRef.setProperty("escape", "NoEscape");
						thisRef.createRelationshipTo(neoNode, RelTypes.POINTSTO);
					}
					
					ObjectNode objNode = (ObjectNode) cgnode;
					for (FieldNode fldNode : objNode.getFieldNodes()) {
						Node fNeoNode = graphDb.createNode(DynamicLabel.label("Field"));
						fNeoNode.setProperty("method", mtdSig);
						fNeoNode.setProperty("astnode", fldNode.toNeo4jString());
						fNeoNode.setProperty("escape", fldNode.getEscapeState().toString());
						fNeoNode.setProperty("owner", objNode.toNeo4jString());
						neoNode.createRelationshipTo(fNeoNode, RelTypes.OWNS);
					}
				}
			}
			for (CGNode cgnode : cg.getNodes()) {				
				if (cgnode instanceof ObjectNode)
					for (FieldNode fldNode : ((ObjectNode) cgnode).getFieldNodes())
						handleRefNode((RefNode) fldNode, mtdSig);
				else if (cgnode instanceof RefNode)
					handleRefNode((RefNode) cgnode, mtdSig);
				else
					CommonUtils.printlnError("Should not be here!");
			}
			
			tx.success();
		}
	}
	
	// helper
	private void handleRefNode(RefNode refNode, String mtdSig) {
		Node neoNodeFrom = findCorrespondingNeoNode(refNode, mtdSig);
		for (Entry<CGNode, EdgeType> entry : refNode.getOutEdges().entrySet()) {
			Node neoNodeTo = findCorrespondingNeoNode(entry.getKey(), mtdSig);
			if (entry.getValue() == EdgeType.DeferredEdge)
				neoNodeFrom.createRelationshipTo(neoNodeTo, RelTypes.REFERSTO);
			else if (entry.getValue() == EdgeType.PointsToEdge)
				neoNodeFrom.createRelationshipTo(neoNodeTo, RelTypes.POINTSTO);
			else
				CommonUtils.printlnError("Should not be here!");
		}
	}
	
	private Node findCorrespondingNeoNode(CGNode cgnode, String mtdSig) {
		ResourceIterator<Node> nodesIter = null;
		if (cgnode instanceof ParamVarNode)
			nodesIter = graphDb.findNodes(DynamicLabel.label("ParamVar"), "method", mtdSig);
		else if (cgnode instanceof LocalVarNode)
			nodesIter = graphDb.findNodes(DynamicLabel.label("LocalVar"), "method", mtdSig);
		else if (cgnode instanceof ObjectNode)
			nodesIter = graphDb.findNodes(DynamicLabel.label("Object"), "method", mtdSig);
		else if (cgnode instanceof FieldNode)
			nodesIter = graphDb.findNodes(DynamicLabel.label("Field"), "method", mtdSig);
		
		while (nodesIter.hasNext()) {
			Node candidate = nodesIter.next();
			String astnodeStr = (String) candidate.getProperty("astnode");
			if (!astnodeStr.equals(cgnode.toNeo4jString()))
				continue;
			
			String es = (String) candidate.getProperty("escape");
			if (!es.equals(cgnode.getEscapeState().toString()))
				continue;
			
			if (cgnode instanceof FieldNode) {
				String candidateOwnerStr = (String) candidate.getProperty("owner");
				String fieldOwnerStr = (String) ((FieldNode) cgnode).getOwner().toNeo4jString();
				if (!candidateOwnerStr.equals(fieldOwnerStr))
					continue;
			}
			
			return candidate;
		}
		
		return null;
	}
	
	private static boolean compareProperties(Node one, Node another) {
		Map<String, Object> props0 = one.getAllProperties();
		Map<String, Object> props1 = another.getAllProperties();
		
		if (props0.size() != props1.size())
			return false;
		
		for (Entry<String, Object> entry0 : props0.entrySet()) {
			String key0 = entry0.getKey();
			if (!another.hasProperty(key0))
				return false;
			
			Object val0 = entry0.getValue();
			Object val1 = another.getProperty(key0);
			if (!val0.equals(val1))
				return false;
		}
		
		return true;
	}

	public void test() {
		try (Transaction tx = graphDb.beginTx()) {
			// Database operations go here
			
			Node nameParam = graphDb.createNode(DynamicLabel.label("ParamVarNode"));
			nameParam.setProperty("method", "Person.Person(Person)");
			nameParam.setProperty("astnode", "name");
			nameParam.setProperty("escape", "ArgEscape");
			
			Node addrParam = graphDb.createNode(DynamicLabel.label("ParamVarNode"));
			addrParam.setProperty("method", "Person.Person(Person)");
			addrParam.setProperty("astnode", "addr");
			addrParam.setProperty("escape", "ArgEscape");
			
			Node Person = graphDb.createNode(DynamicLabel.label("ObjectNode_TPD"));
			Person.setProperty("method", "Person.Person(Person)");
			Person.setProperty("astnode", "Person");
			Person.setProperty("escape", "NoEscape");
			
			Node nameField = graphDb.createNode(DynamicLabel.label("FieldNode"));
			nameField.setProperty("method", "Person.Person(Person)");
			nameField.setProperty("astnode", "name");
			nameField.setProperty("escape", "NoEscape");
			
			Node addrField = graphDb.createNode(DynamicLabel.label("FieldNode"));
			addrField.setProperty("method", "Person.Person(Person)");
			addrField.setProperty("astnode", "addr");
			addrField.setProperty("escape", "NoEscape");

			Relationship relationship0 = Person.createRelationshipTo(nameField, RelTypes.OWNS);
			relationship0.setProperty("access", "private");
			
			Relationship relationship1 = Person.createRelationshipTo(addrField, RelTypes.OWNS);
			relationship1.setProperty("access", "private");
			
			Relationship relationship2 = nameField.createRelationshipTo(nameParam, RelTypes.REFERSTO);
			
			Relationship relationship3 = addrField.createRelationshipTo(addrParam, RelTypes.REFERSTO);
			
			tx.success();
		}
	}
	
}
