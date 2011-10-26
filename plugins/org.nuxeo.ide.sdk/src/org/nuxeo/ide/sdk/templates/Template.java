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
package org.nuxeo.ide.sdk.templates;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.nuxeo.ide.common.IOUtils;
import org.nuxeo.ide.sdk.SDKPlugin;
import org.nuxeo.ide.sdk.features.FeatureTemplateContext;
import org.nuxeo.ide.sdk.model.ExtensionModel;
import org.nuxeo.ide.sdk.templates.cmd.Command;
import org.nuxeo.ide.sdk.templates.cmd.PostCreateCommand;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Template implements Comparable<Template> {

    protected String id;

    protected String name;

    protected String description;

    protected String src;

    protected List<Command> commands;

    protected List<PostCreateCommand> postCreateCommands;

    /**
     * @deprecated replace this by using commands
     */
    protected String extensions;

    protected Template(String id) {
        this.id = id;
        this.name = id;
        this.commands = new ArrayList<Command>();
        this.postCreateCommands = new ArrayList<PostCreateCommand>();
    }

    /**
     * @deprecated
     * @param extensions
     */
    public void setExtensions(String extensions) {
        this.extensions = extensions;
    }

    /**
     * Return null if no extensions are contributed
     * 
     * @deprecated
     * @return
     */
    public String getExtensions() {
        return extensions;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public List<PostCreateCommand> getPostCreateCommands() {
        return postCreateCommands;
    }

    public String getId() {
        return id;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getSrc() {
        return src;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return src;
    }

    /**
     * Expand and process the templates into a temporary target directory
     * 
     * @param bundle
     * @param ctx
     * @param dir
     * @throws Exception
     */
    public void process(Bundle bundle, TemplateContext ctx, File dir)
            throws Exception {
        File tmp = IOUtils.createTempDir(dir.getParentFile());
        try {
            copyTo(bundle, tmp);
            installTemplate(tmp, dir);
            processCommands(bundle, ctx, dir);
        } finally {
            IOUtils.deleteTree(tmp);
        }
    }

    /**
     * Post processing of the creation template. This method needs to be run in
     * a JDT context (i.e. IProject argument).
     * 
     * @param project
     * @param ctx
     */
    public void postProcess(IProject project, TemplateContext ctx)
            throws Exception {
        for (PostCreateCommand cmd : postCreateCommands) {
            cmd.execute(project, ctx);
        }
    }

    /**
     * Process the template commands against the given directory (this is usual
     * a temporary directory used for processing the template)
     * 
     * @param bundle
     * @param ctx
     * @param dir
     * @throws Exception
     */
    protected void processCommands(Bundle bundle, TemplateContext ctx, File dir)
            throws Exception {
        for (Command cmd : commands) {
            cmd.execute(ctx, bundle, dir);
        }
        applyExtensions(bundle, ctx, dir);
    }

    /**
     * The template artifacts were expanded into a tmp directory. Install these
     * artifacts in the final directory.
     * 
     * @param tmp - the temporary directory containing the generated artifacts
     * @param dir - the target directory (may not exists)
     */
    protected void installTemplate(File tmp, File dir) throws IOException {
        if (!dir.isDirectory()) {
            dir.getParentFile().mkdir();
            tmp.renameTo(dir);
        } else {
            IOUtils.copyTreeContent(tmp, dir, false);
        }
    }

    protected void copyTo(Bundle bundle, File dir) throws IOException {
        if (src.endsWith(".zip")) {
            URL url = bundle.getEntry(src);
            if (url == null) {
                throw new FileNotFoundException("Template " + name
                        + " not found in bundle " + bundle.getSymbolicName()
                        + " at " + src);
            }
            InputStream in = url.openStream();
            try {
                IOUtils.unzip(in, dir);
            } finally {
                in.close();
            }
        } else {
            File bundleFile = FileLocator.getBundleFile(bundle);
            if (bundleFile.isDirectory()) {
                IOUtils.copyTreeContent(new File(bundleFile, src), dir, false);
            } else {
                IOUtils.unzip(bundleFile, src, dir, false);
            }
        }
    }

    public static void loadCommand(TemplateManager manager,
            List<Command> commands, List<PostCreateCommand> postCreateCommands,
            Element el, String tag) throws Exception {
        ElementHandler cmd = manager.getCommand(tag);
        if (cmd == null) {
            throw new IllegalArgumentException("Unknown command: " + tag);
        }
        cmd.init(el);
        boolean knownType = false;
        if (cmd instanceof Command) {
            commands.add((Command) cmd);
            knownType = true;
        }
        if (cmd instanceof PostCreateCommand) {
            postCreateCommands.add((PostCreateCommand) cmd);
            knownType = true;
        }
        if (!knownType) {
            throw new IllegalArgumentException("Unknown command type: " + tag
                    + " -> " + cmd);
        }
    }

    @Override
    public int compareTo(Template temp) {
        return name.compareTo(temp.name);
    }

    @Override
    public String toString() {
        return id + " [" + src + "]";
    }

    protected void applyExtensions(Bundle bundle, TemplateContext ctx, File dir)
            throws Exception {
        if (extensions == null) {
            return;
        }
        String className = ctx.getProperty(FeatureTemplateContext.CLASS_NAME);
        if (className == null) {
            return;
        }
        String packageName = ctx.getProperty(FeatureTemplateContext.PACKAGE_NAME);
        if (packageName == null) {
            return;
        }
        TemplateEngine engine = SDKPlugin.getDefault().getTemplateManager().getEngine();
        URL url = bundle.getEntry(extensions);
        if (url != null) {
            String content = IOUtils.read(url);
            content = engine.expandVars(ctx, content);
            new ExtensionModel(packageName + "." + className).setContent(dir,
                    content);
        }
    }

    public static Template load(TemplateManager manager, Element element)
            throws Exception {
        Template temp = new Template(element.getAttribute("id"));
        Node child = element.getFirstChild();
        temp.src = Util.getAttribute(element, "src");
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) child;
                String tag = child.getNodeName();
                if ("name".equals(tag)) {
                    temp.name = el.getTextContent().trim();
                } else if ("description".equals(tag)) {
                    temp.description = el.getTextContent().trim();
                } else if ("extension".equals(tag)) {
                    temp.extensions = el.getAttribute("src").trim();
                } else {
                    loadCommand(manager, temp.commands,
                            temp.postCreateCommands, el, tag);
                }
            }
            child = child.getNextSibling();
        }
        return temp;
    }

}
