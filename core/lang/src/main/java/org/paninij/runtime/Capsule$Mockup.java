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
package org.paninij.runtime;

public class Capsule$Mockup implements Panini$Capsule
{
    @Override
    public void panini$start() {
        /* Do nothing. */
    }

    @Override
    public void panini$push(Object o) {
        /* Do nothing. */
    }

    @Override
    public void panini$join() throws InterruptedException {
        /* Do nothing. */
    }

    @Override
    public void panini$openLink() {
        /* Do nothing. */
    }

    @Override
    public void panini$closeLink() {
        /* Do nothing. */
    }

    @Override
    public void exit()
    {
        /* Do nothing. */
    }

    @Override
    public void yield(long millis) {
        /* Do nothing. */
    }
}