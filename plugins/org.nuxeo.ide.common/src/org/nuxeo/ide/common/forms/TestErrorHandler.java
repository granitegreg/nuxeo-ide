/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ide.common.forms;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class TestErrorHandler implements ErrorHandler {

    @Override
    public void showError(UIObject<?> obj, String error) {
        System.out.println("Error on " + obj.getId() + ": " + error);
    }

    @Override
    public void hideError(UIObject<?> obj) {
        System.out.println("No more errors on " + obj.getId());
    }

    @Override
    public void setErrorCount(int count) {
        System.out.println("The are still " + count + " errors");
    }

}
