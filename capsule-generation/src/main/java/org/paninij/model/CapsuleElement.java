/*
 * This file is part of the Panini project at Iowa State University.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * For more details and the latest version of this code please see
 * http://paninij.org
 *
 * Contributor(s): Dalton Mills
 */
package org.paninij.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.paninij.apt.CapsuleTemplateVisitor;
import org.paninij.apt.util.PaniniModelInfo;
import org.paninij.apt.util.TypeCollector;

public class CapsuleElement implements Capsule
{
    private String simpleName;
    private String qualifiedName;
    private TypeElement element;
    private ArrayList<Procedure> procedures;
    private ArrayList<Variable> children;

    /*
     * Generate a Capsule from a TypeElement. The TypeElement should already be checked for
     * any errors. The TypeElement should be annotated with @Capsule and should represent
     * a CapsuleTemplate
     */
    public static Capsule make(TypeElement e) {
        CapsuleElement capsule = new CapsuleElement();
        CapsuleTemplateVisitor visitor = new CapsuleTemplateVisitor();
        e.accept(visitor, capsule);
        return capsule;
    }

    private CapsuleElement() {
        this.simpleName = "";
        this.qualifiedName = "";
        this.element = null;
        this.procedures = new ArrayList<Procedure>();
        this.children = new ArrayList<Variable>();
    }

    @Override
    public List<Variable> getChildren() {
        return this.children;
    }

    public void addChild(Variable v) {
        System.out.println("CHILD: " + v.toString());
        this.children.add(v);
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
    public List<Procedure> getProcedures() {
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

    @Override
    public ArrayList<String> getSignatures() {
        ArrayList<String> sigs = new ArrayList<String>();

        for (TypeMirror i : this.element.getInterfaces()) {
            String name = i.toString();
            assert(name.endsWith(PaniniModelInfo.SIGNATURE_TEMPLATE_SUFFIX));
            name = name.substring(0, name.length() - PaniniModelInfo.SIGNATURE_TEMPLATE_SUFFIX.length());
            sigs.add(name);
        }
        return sigs;
    }

    public void addExecutable(ExecutableElement e) {
        if (PaniniModelInfo.isProcedure(e)) {
            this.procedures.add(new ProcedureElement(e));
        }
    }

    public void setTypeElement(TypeElement e) {
        if (this.element == null) {
            this.element = e;
            this.simpleName = PaniniModelInfo.simpleCapsuleName(e);
            this.qualifiedName = PaniniModelInfo.qualifiedCapsuleName(e);
        }
    }
}
