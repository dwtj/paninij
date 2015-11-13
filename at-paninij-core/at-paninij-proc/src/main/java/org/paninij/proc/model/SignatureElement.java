package org.paninij.proc.model;

import java.util.ArrayList;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import org.paninij.proc.util.PaniniModel;
import org.paninij.proc.util.TypeCollector;

//TODO
public class SignatureElement implements Signature
{
    private String simpleName;
    private String qualifiedName;
    private TypeElement element;
    private ArrayList<Procedure> procedures;

    public static Signature make(TypeElement e) {
        SignatureElement signature = new SignatureElement();
        SignatureTemplateVisitor visitor = new SignatureTemplateVisitor();
        e.accept(visitor,  signature);
        return signature;
    }

    private SignatureElement() {
        this.simpleName = "";
        this.qualifiedName = "";
        this.element = null;
        this.procedures = new ArrayList<Procedure>();
    }

    @Override
    public String getSimpleName() {
        return this.simpleName;
    }

    @Override
    public String getQualifiedName() {
        return this.qualifiedName;
    }

    @Override
    public ArrayList<Procedure> getProcedures() {
        return this.procedures;
    }

    @Override
    public Set<String> getImports() {
        return TypeCollector.collect(this.element);
    }

    @Override
    public String getPackage() {
        PackageElement pack = (PackageElement) this.element.getEnclosingElement();
        return pack.getQualifiedName().toString();
    }

    public void setTypeElement(TypeElement e) {
        if (this.element == null) {
            this.element = e;
            this.simpleName = PaniniModel.simpleSignatureName(e);
            this.qualifiedName = PaniniModel.qualifiedSignatureName(e);
        }
    }

    public void addExecutable(ExecutableElement e) {
        if (PaniniModel.isProcedure(e)) {
            this.procedures.add(new ProcedureElement(e));
        }
    }
}