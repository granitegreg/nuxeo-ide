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
package org.nuxeo.ide.common.forms.model;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.nuxeo.ide.common.forms.BindingContext;
import org.nuxeo.ide.common.forms.HasText;
import org.nuxeo.ide.common.forms.UIObject;
import org.nuxeo.ide.common.forms.WidgetName;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@WidgetName("text")
public class TextWidget extends UIObject<Label> implements HasText {

    @Override
    protected Label createControl(Composite parent, Element element,
            BindingContext ctx) {
        boolean wrap = getBooleanAttribute(element, "wrap", false);
        int style = SWT.NONE;
        if (wrap) {
            style |= SWT.WRAP;
        }
        return new Label(parent, style);
    }

    @Override
    public String getText() {
        return ctrl.getText();
    }

    @Override
    public void setText(String text) {
        ctrl.setText(text);
    }

}
