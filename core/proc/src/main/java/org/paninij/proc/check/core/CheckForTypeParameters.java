/*******************************************************************************
 * This file is part of the Panini project at Iowa State University.
 *
 * @PaniniJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * @PaniniJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with @PaniniJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more details and the latest version of this code please see
 * http://paninij.org
 *
 * Contributors:
 * 	Dr. Hridesh Rajan,
 * 	Dalton Mills,
 * 	David Johnston,
 * 	Trey Erenberger
 *******************************************************************************/
package org.paninij.proc.check.core;

import static org.paninij.proc.check.Check.Result.OK;
import static org.paninij.proc.check.Check.Result.error;

import javax.lang.model.element.TypeElement;


/**
 * Check that a core does not have any type parameters.
 */
public class CheckForTypeParameters implements CoreCheck
{
    @Override
    public Result checkCore(TypeElement core, CoreKind coreKind)
    {
        if (!core.getTypeParameters().isEmpty())
        {
            String err = "A {0} core must not have any type parameters.";
            err = String.format(err, coreKind);
            return error(err, CheckForTypeParameters.class, core);
        }
        return OK;
    }
}
