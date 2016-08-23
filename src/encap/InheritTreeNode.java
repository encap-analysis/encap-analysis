package encap;

import java.util.Set;

import org.eclipse.jdt.core.dom.TypeDeclaration;

public class InheritTreeNode implements Cloneable {
	protected TypeDeclaration tpd = null;

	protected InheritTreeNode superClass = null;
	protected Set<InheritTreeNode> subClasses = CommonUtils.newHashSet();

	InheritTreeNode(TypeDeclaration type) {
		tpd = type;
	}
	
	boolean correspondTo(TypeDeclaration type) {
		if (type != null)
			if (tpd.getStartPosition() == type.getStartPosition() && tpd.getLength() == type.getLength())
				return true;
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		InheritTreeNode other = (InheritTreeNode) obj;

		return correspondTo(other.tpd);
	}
	
	@ Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("CGN: " + CommonUtils.getTpdName(tpd) + "\n");
		sb.append("Super class: \n");
		sb.append("\t").append(CommonUtils.getTpdName(superClass.tpd)).append("\n");
		sb.append("Sub classes: \n");
		for (InheritTreeNode cgn : subClasses) {
			sb.append("\t").append(CommonUtils.getTpdName(cgn.tpd)).append("\n");
		}
		return sb.toString();
	}
	
	public String toSimpleStr() {
		return CommonUtils.getTpdName(tpd);
	}

	public TypeDeclaration getTpd() {
		return tpd;
	}

	public InheritTreeNode getSuperClass() {
		return superClass;
	}

	public Set<InheritTreeNode> getSubClasses() {
		return subClasses;
	}
}
