package simplejpa.scaffolding

import org.codehaus.groovy.antlr.AntlrASTProcessor
import org.codehaus.groovy.antlr.GroovySourceAST
import org.codehaus.groovy.antlr.SourceBuffer
import org.codehaus.groovy.antlr.UnicodeEscapingReader
import org.codehaus.groovy.antlr.parser.GroovyLexer
import org.codehaus.groovy.antlr.parser.GroovyRecognizer
import org.codehaus.groovy.antlr.treewalker.SourceCodeTraversal
import org.codehaus.groovy.antlr.treewalker.Visitor
import org.codehaus.groovy.antlr.treewalker.VisitorAdapter
import simplejpa.scaffolding.attribute.Attribute
import simplejpa.scaffolding.attribute.BasicAttribute
import simplejpa.scaffolding.attribute.CollectionAttribute
import simplejpa.scaffolding.attribute.DateAttribute
import simplejpa.scaffolding.attribute.EntityAttribute
import simplejpa.scaffolding.attribute.EnumeratedAttribute
import simplejpa.scaffolding.attribute.UnknownAttribute
import javax.persistence.FetchType
import static org.codehaus.groovy.antlr.parser.GroovyTokenTypes.*
import griffon.util.*

class DomainClass extends VisitorAdapter {

	private static List SUPPORTED_ATTRIBUTES = [BasicAttribute, DateAttribute, EntityAttribute, EnumeratedAttribute, CollectionAttribute]

	String name
	String nameAsPair
	String nameAsChild
    String nameAsProperty
	String parentClassName
    String sourceClass
	String packageName
	String targetPackage
	File file
	List<Attribute> attributes = []
	List<Annotation> annotations = []

	public DomainClass(File domainClassFile, String domainPackageName, String targetPackage) {

		this.file = domainClassFile
		this.targetPackage = targetPackage

		//
		// Parse class from source file
		//
		SourceBuffer sourceBuffer = new SourceBuffer()
		UnicodeEscapingReader unicodeEscapingReader = new UnicodeEscapingReader(new FileReader(domainClassFile), sourceBuffer)
		GroovyLexer lexer = new GroovyLexer(unicodeEscapingReader)
		unicodeEscapingReader.setLexer(lexer)
		GroovyRecognizer parser = GroovyRecognizer.make(lexer)
		parser.setSourceBuffer(sourceBuffer)
		parser.compilationUnit()
		GroovySourceAST ast = parser.getAST()
		AntlrASTProcessor traverser = new SourceCodeTraversal(this)
		traverser.process(ast)


		//
		// Convert the parsed information into DOM attributes
		//
		fields.each { Map information ->
			Attribute attribute
			for (Class c: SUPPORTED_ATTRIBUTES) {
				if (c.isInstanceOf(information)) {
					attribute = c.newInstance([information].toArray())
					break;
				}
			}
			if (attribute == null) {
				attribute = new UnknownAttribute(information['name'], information['type'])
			}
			attributes << attribute
		}

		//
		// Find package name
		//
		def pattern = ".*${domainPackageName.replace('.','/')}(.*)/${name}.*"
		def matcher = domainClassFile =~ pattern
		if (matcher.matches()) {
			packageName = "${domainPackageName}${matcher.group(1).replace('/','.')}"
		} else {
			packageName = domainPackageName
		}

		nameAsPair = name + "AsPair"
		nameAsChild = name + "AsChild"
        nameAsProperty = GriffonNameUtils.getPropertyName(name as String)
	}

	public boolean isEntity() {
		annotations.find { it.name == 'Entity' }
	}

    public List<Attribute> getOneToManyInverse() {
        attributes.findAll { it instanceof CollectionAttribute && it.oneToMany && it.bidirectional }
    }

    public List<Attribute> getManyToManyInverse() {
        attributes.findAll { it instanceof CollectionAttribute && it.manyToMany && it.bidirectional }
    }

	//
	// Visitor
	//

	private boolean inClass = true
	private boolean ignoreMode = false
	private List fields = []
	private Annotation currentAnnotation

    void visitClassDef(GroovySourceAST node, int visitType) {
		if (visitType==Visitor.OPENING_VISIT && inClass) {
			name = node.childOfType(IDENT).text
			def extendsClass = node.childOfType(EXTENDS_CLAUSE)
			if (extendsClass != null && extendsClass.numberOfChildren > 0) {
				parentClassName = extendsClass.childAt(0).text
			}
		}
	}

	void visitExtendsClause(GroovySourceAST t, int visit) {
		super.visitExtendsClause(t, visit)
	}

	public void visitVariableDef(GroovySourceAST node, int visitType) {
		inClass = false
		if (visitType==Visitor.OPENING_VISIT && !ignoreMode)  {
			fields << ['name': node.childOfType(IDENT).text, 'type': node.childOfType(TYPE).childAt(0).text]
			List typeArguments = node.childOfType(TYPE).childAt(0).childrenOfType(TYPE_ARGUMENTS)
			if (!typeArguments.isEmpty()) {
				fields.last().'typeArgument' = typeArguments[0]?.childAt(0)?.childAt(0)?.childAt(0)
			}
		}
	}

	public void visitMethodDef(GroovySourceAST node, int visitType) {
		if (visitType==Visitor.OPENING_VISIT) {
			ignoreMode = true
		} else if (visitType==Visitor.CLOSING_VISIT) {
			ignoreMode = false
		}
	}

	public void visitAnnotation(GroovySourceAST node, int visitType) {
		if (visitType==Visitor.OPENING_VISIT) {
			currentAnnotation = new Annotation(node.childAt(0).text)
			if (inClass) {
				annotations << currentAnnotation
			} else {
				if (!fields.last().'annotations') {
					fields.last().'annotations' = []
				}
				fields.last().'annotations' << currentAnnotation
			}
		}
	}

	public void visitAnnotationMemberValuePair(GroovySourceAST node, int visitType) {
		if (visitType==Visitor.OPENING_VISIT) {
			String member = node.childAt(0).toString()
			if (node.childAt(1) != null) {
                if (member == 'fetch') {
                    if (node.childAt(1).childAt(1).text == "EAGER") {
                        currentAnnotation.addMember(member, FetchType.EAGER)
                    } else if (node.childAt(1).childAt(1).text == "LAZY") {
                        currentAnnotation.addMember(member, FetchType.LAZY)
                    }
                } else {
                    currentAnnotation.addMember(member, node.childAt(1).text)
                }
			}
		}
	}

}