package encap;

import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.SimpleName;

public class FieldNode extends RefNode {	
	private ObjectNode ownerObjNode = null;

	FieldNode(ObjectNode objNode, SimpleName fieldName) {
		super(fieldName);
		this.ownerObjNode = objNode;
	}
	
	ObjectNode getOwner() {
		return ownerObjNode;
	}
	
	public String getSimpleNameStr() {
		return astnode.toString();
	}

	public SimpleName getSimpleName() {
		return (SimpleName)astnode;
	}

	public String toSimpleString() {
		return "[FieldNode of " + ownerObjNode.getSimpleNameStr() + ": " + astnode + "]";
	}
	
	public String toInputOutputString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(" + escape + ")\t");
		sb.append("In: {");
		for (Entry<CGNode, EdgeType> entry : inEdges.entrySet()) {
			sb.append(entry.getKey().toSimpleString()).append(",");
		}
		sb.append("}\tOut: {");
		for (Entry<CGNode, EdgeType> entry : outEdges.entrySet()) {
			sb.append(entry.getKey().toSimpleString()).append(",");
		}
		sb.append("}");
		
		return sb.toString();
	}

	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Field " + astnode + ": \n");
		sb.append("Owner: " + ownerObjNode.getSimpleNameStr()).append("\n");		
		sb.append(toInputOutputString());
		
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 2;
		result = prime * result + ((astnode == null) ? 0 : hashIndex);
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		FieldNode fldNode = (FieldNode)obj;
		
		if (!this.correspondTo(fldNode.astnode))
			return false;
		
		if (!this.getOwner().correspondTo(fldNode.getOwner().getASTNode()))
			return false;
		
		if (this.inEdges.size() != fldNode.inEdges.size() || this.outEdges.size() != fldNode.outEdges.size())
			return false;
		
		return true;
	}

	@Override
	public boolean isEqualTo(Object obj) {
		if (!equals(obj))
			return false;
		FieldNode fldNode = (FieldNode)obj;
		
		if (this.escape != fldNode.escape 
			|| this.inEdges.size() != fldNode.inEdges.size() 
			|| this.outEdges.size() != fldNode.outEdges.size())
			return false;
		
		return this.inEdges.equals(fldNode.inEdges) && this.outEdges.equals(fldNode.outEdges);
	}

	@Override
	public NodeType getNodeType() {
		return NodeType.Field;
	}

	@Override
	public Object clone() {
		CommonUtils.printlnError("The method clone() of FieldNode should not be used!");
		return null;
	}

	@Override
	public String toNeo4jString() {
		return getSimpleNameStr();
	}
}
