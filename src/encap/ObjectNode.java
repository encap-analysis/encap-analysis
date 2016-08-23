package encap;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

enum ObjectType {
	TPD, SVD, CIC
}
public class ObjectNode extends CGNode {
	
	private Set<FieldNode> fields = CommonUtils.newHashSet();
	
	private ObjectNode superObj = null;
	private ObjectNode outerObj = null;
	
	private boolean isPhantom = false;
	private ObjectType type = null;
	
//	// T.f
//	public ObjectNode(SimpleName className) {
//		super(className);
//	}
	
	// Correspond to a class
	public ObjectNode(TypeDeclaration tpd) {
		super(tpd);
		isPhantom = true;
		type = ObjectType.TPD;
		initFieldNodes(false);
	}
	
	public ObjectNode(TypeDeclaration tpd, boolean bHandleInitializer) {
		super(tpd);
		isPhantom = true;
		type = ObjectType.TPD;
		initFieldNodes(bHandleInitializer);
	}
	
	// Correspond to a peer object
	public ObjectNode(SingleVariableDeclaration svd) {
		super(svd);
		isPhantom = true;
		type = ObjectType.SVD;
		initFieldNodes(false);
	}
	
	public ObjectNode(SingleVariableDeclaration svd, boolean bHandleInitializer) {
		super(svd);
		isPhantom = true;
		type = ObjectType.SVD;
		initFieldNodes(bHandleInitializer);
	}

	// new T()
	// Correspond to a new instance
	public ObjectNode(ClassInstanceCreation cic) {
		super(cic);
		isPhantom = false;
		type = ObjectType.CIC;
		initFieldNodes(false);
	}
	
	public ObjectNode(ClassInstanceCreation cic, boolean bHandleInitializer) {
		super(cic);
		isPhantom = false;
		type = ObjectType.CIC;
		initFieldNodes(bHandleInitializer);
	}
	
	// Init all fields
	private void initFieldNodes(boolean bHandleInitializer) {
		TypeDeclaration tpd = null;
		if (astnode instanceof TypeDeclaration)
			tpd = (TypeDeclaration)astnode;
		else if (astnode instanceof ClassInstanceCreation)
			tpd = CommonUtils.getTyd((ClassInstanceCreation)astnode);
		else if (astnode instanceof SingleVariableDeclaration)
			tpd = CommonUtils.getTyd((SingleVariableDeclaration)astnode);
		else
			CommonUtils.printlnError("Wrong type of astnode!");
		
		if (tpd == null) {
//			Utils.printlnError("TypeDeclaration for <" + Utils.getTypeName(astnode) + "> not found!");
			return;
		}
		
		FieldDeclaration[] flds = tpd.getFields();
		for (FieldDeclaration fld : flds) {
			fld.getType().isPrimitiveType();
			for (VariableDeclarationFragment vdf : (List<VariableDeclarationFragment>)fld.fragments()) {
				
				SimpleName fieldName = vdf.getName();
				if (fieldName == null)
					CommonUtils.debug("incorrect field name.");
				if (CommonUtils.isPrimitive(fieldName) == null || CommonUtils.isPrimitive(fieldName))
					continue;
				
				CGNode fieldNode = addFieldNode(fieldName);
				if (CommonUtils.isStatic(fieldName) || CommonUtils.isPublic(fieldName))
					fieldNode.adjustEscapeState(EscapeState.GlobalEscape);
				
				if (bHandleInitializer) {
					// avoid dead recursions
					Expression initializer = vdf.getInitializer();
					// T p;
					// T p = null;
					if (initializer == null || initializer instanceof NullLiteral)
						continue;
					// T p = new T;
					if (initializer instanceof ClassInstanceCreation) {
						TypeDeclaration otherTPD = CommonUtils.getTyd((ClassInstanceCreation)initializer);
						ObjectNode node = null;
						if (correspondTo(otherTPD))
							node = new ObjectNode((ClassInstanceCreation)initializer, false);
						else
							node = new ObjectNode((ClassInstanceCreation)initializer, true);
						addOutEdge(fieldName, node, EdgeType.PointsToEdge);
					}
					// T p = f();
					else if (initializer instanceof MethodInvocation) {
					}
				}
			}
		}		
		
	}
	
	void setSuperObj(ObjectNode obj) {
		this.superObj = obj;
	}
	
	void setOuterObj(ObjectNode obj) {
		this.outerObj = obj;
	}
	
	FieldNode addFieldNode(SimpleName fieldName) {
		FieldNode node = new FieldNode(this, fieldName);
		fields.add(node);
		return node;
	}
	
	// Get the FieldNode of a SimpleName
	public FieldNode getFieldNode(SimpleName fieldName) {
		// find in the current class
		FieldNode fld = getFieldNodeHelper(fieldName);
		if (fld != null)
			return fld;
		
		// find in the super class
		ObjectNode superObj = this.superObj;
		while (fld == null && superObj != null) {
			fld = superObj.getFieldNodeHelper(fieldName);
			superObj = superObj.superObj;
		}
		
		if (fld != null)
			return fld;
		
		// find in the outer class
		ObjectNode outerObj = this.outerObj;
		while (fld == null && outerObj != null) {
			fld = superObj.getFieldNodeHelper(fieldName);
			outerObj = outerObj.outerObj;
		}
		
		if (fld != null)
			return fld;
		
		return null;
	}
	
	private FieldNode getFieldNodeHelper(SimpleName fieldName) {
		for (FieldNode fieldNode : fields) {
			if (fieldNode.correspondTo(fieldName))
				return fieldNode;
		}
		return null;
	}
	
	public Set<FieldNode> getFieldNodes() {
		return fields;
	}
	
	Set<CGNode> getPointsToNode(SimpleName fieldName) {
		Set<CGNode> nodes = CommonUtils.newHashSet();
		FieldNode fieldNode = getFieldNode(fieldName);
		nodes.addAll(fieldNode.getPointsToNode());

		return nodes;
	}

	@Override
	Set<ObjectNode> getPointsToNode() {
		Set<ObjectNode> nodes = CommonUtils.newHashSet();
		for (FieldNode fieldNode : fields)
			nodes.addAll(fieldNode.getPointsToNode());

		return nodes;
	}
	
	@ Override
	public boolean correspondTo(ASTNode astnode) {
		if (astnode == null) {
			if (this.astnode == null)
				return true;
			return false;
		} else if (this.astnode == null) {
			return false;
		}		
		if (this.astnode == astnode)
			return true;
		
		if (isPhantom() && astnode instanceof TypeDeclaration)
			return this.astnode.subtreeMatch(new ASTMatcher(), astnode);
		else		
			return this.astnode.getStartPosition() == astnode.getStartPosition() 
					&& this.astnode.getLength() == astnode.getLength();
	}

	@ Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		ObjectNode objNode = (ObjectNode)obj;		
		return this.correspondTo(objNode.astnode);
	}
	
	@ Override
	public boolean isEqualTo(Object obj) {
		if (!equals(obj))
			return false;
		
		ObjectNode objNode = (ObjectNode)obj;
		if (this.escape != objNode.escape 
			|| this.inEdges.size() != objNode.inEdges.size() 
			|| this.fields.size() != objNode.fields.size())
			return false;
		
		return this.inEdges.equals(objNode.inEdges) && this.fields.equals(objNode.fields);
	}
	
	public String getSimpleNameStr() {
		if (astnode instanceof SimpleName)
			return ((SimpleName)astnode).toString();
		else if (astnode instanceof TypeDeclaration)
			return "(TYD) " + ((TypeDeclaration)astnode).getName().toString();
		else if (astnode instanceof SingleVariableDeclaration)
			return "(SVD) " + ((SingleVariableDeclaration)astnode).toString();
		else if (astnode instanceof ClassInstanceCreation)
			return "(CIC) " + ((ClassInstanceCreation)astnode).toString() + " (" + astnode.getStartPosition() + ", " + astnode.getLength() + ")";
		
		return null;
	}

	@ Override
	public String toSimpleString() {
		return "[JavaObjectNode: " + (astnode == null ? "null" : getSimpleNameStr()) + "]";
	}

	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NodeType: ").append("JavaObjectNode (").append(astnode.getClass().getSimpleName()).append(")\n");
		sb.append("ASTNode: ").append(getSimpleNameStr()).append("\n");
		sb.append("EscapeState: ").append(escape).append("\n");
		sb.append("isPhantom: ").append(isPhantom).append("\n");
		sb.append("In:\n");
		for (Entry<CGNode, EdgeType> entry : inEdges.entrySet()) {
			sb.append("\t").append(entry.getKey().toSimpleString()).append("\n");
		}
		sb.append("Fields: \n");
		for (FieldNode field : fields) {
			sb.append("\t").append(field.getSimpleNameStr()).append("\t").append(field.toInputOutputString()).append("\n");
		}
		
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((astnode == null) ? 0 : astnode.getStartPosition());
		result = prime * result + ((astnode == null) ? 0 : astnode.getLength());
		
		return result;
	}

	@Override
	void merge(CGNode cgnode) {
		if (cgnode instanceof ObjectNode)
			adjustEscapeState(cgnode.getEscapeState());
		
		ObjectNode objNode = (ObjectNode)cgnode;
		
		// not all fields can be found because of upcast
		for (FieldNode otherFldNode : objNode.getFieldNodes()) {
			FieldNode fldNode = getFieldNode(otherFldNode.getSimpleName());
			if (fldNode != null)
				fldNode.merge(otherFldNode);
		}
	}

	@Override
	public NodeType getNodeType() {
		return NodeType.JavaObject;
	}
	
	@ Override
	public Object clone() {
		ObjectNode objNode = null;
		if (astnode instanceof TypeDeclaration)
			objNode = new ObjectNode((TypeDeclaration)astnode);
		else if (astnode instanceof SingleVariableDeclaration)
			objNode = new ObjectNode((SingleVariableDeclaration)astnode);
		else if (astnode instanceof ClassInstanceCreation)
			objNode = new ObjectNode((ClassInstanceCreation)astnode);
		
		objNode.escape = this.escape;
		
		for (FieldNode fld : objNode.fields) {
			FieldNode otherFld = this.getFieldNode(fld.getSimpleName());
			fld.setEscapeState(otherFld.getEscapeState());
		}
		
		return objNode;
	}
	
	@ Override
	public void addInEdge(CGNode node, EdgeType et) {
		if (node instanceof ObjectNode || et != EdgeType.PointsToEdge)
			return;
		inEdges.put(node, et);
	}
	
	public void addOutEdge(SimpleName fldName, CGNode node, EdgeType et) {
		FieldNode fldNode = getFieldNode(fldName);
		fldNode.addOutEdge(node, et);
	}
	
	public boolean isPhantom() {
		return isPhantom;
	}
	
	public void setPhantom(boolean b) {
		isPhantom = b;
	}
	
	public ObjectType getType() {
		return type;
	}

	@Override
	public String toNeo4jString() {
		return getSimpleNameStr();
	}
}