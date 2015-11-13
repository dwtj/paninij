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
package org.paninij.proc.model;

import java.util.List;

public interface Capsule extends Signature
{
    public List<Variable> getLocalFields();
    public List<Variable> getImportFields();
    public List<Variable> getStateFields();
    public List<String> getSignatures();
    public boolean isRoot();
    public boolean hasInit();
    public boolean hasRun();
    public boolean hasDesign();
    public boolean isActive();
    public boolean hasActiveAncestor();
}