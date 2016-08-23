package encap;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class OuterInnerClassVisitor extends ASTVisitor {
	
	TypeDeclaration outerTypeDec = null;
	
	public OuterInnerClassVisitor(TypeDeclaration tpd) {
		outerTypeDec = tpd;
	}
	
	public boolean visit(TypeDeclaration innerTypeDec) {
		if (innerTypeDec == outerTypeDec)
			return true;

		ITypeBinding innerItb = innerTypeDec.resolveBinding();		
		String innerClassName = innerItb.getQualifiedName();
		
		
		ITypeBinding outerItb = outerTypeDec.resolveBinding();		
		String outerClassName = outerItb.getQualifiedName();
		
		Demo.project.connectInnerAndOuter(innerClassName, outerClassName);
		
		// only access one level
		return false;
	}
	
}
