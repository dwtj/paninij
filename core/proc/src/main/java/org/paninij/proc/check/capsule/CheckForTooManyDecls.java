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
package org.paninij.proc.check.capsule;

import static java.text.MessageFormat.format;
import static javax.lang.model.element.ElementKind.METHOD;

import static org.paninij.proc.check.Check.Result.OK;
import static org.paninij.proc.check.Check.Result.error;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * For each of the three `@PaniniJ` declarations kinds (`run()`, `init()`, and `design()`) this
 * checks that there are only zero or one of declaration of that kind in a capsule core.
 */
public class CheckForTooManyDecls implements CapsuleCheck
{
    @Override
    public Result checkCapsule(TypeElement core)
    {
        int run = 0;
        int init = 0;
        int design = 0;
        
        for (Element elem : core.getEnclosedElements()) {
            if (elem.getKind() == METHOD) {
                switch (elem.getSimpleName().toString()) {
                case "run":
                    run++;
                    continue;
                case "init":
                    init++;
                    continue;
                case "design":
                    design++;
                    continue;
                default:
                    continue;
                }
            }
        }
        if (run > 1) {
            return makeError(core, "run");
        }
        if (init > 1) {
            return makeError(core, "init");
        }
        if (design > 1) {
            return makeError(core, "design");
        }

        return OK;
    }
    
    private static Result makeError(TypeElement core, String decl)
    {
        String err = "Capsule cores must contain either 0 or 1 `{0}()` declarations "
                   + "(i.e. methods).";
        err = format(err, decl);
        return error(err, CheckForTooManyDecls.class, core);
    }
}
