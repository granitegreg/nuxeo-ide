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
package org.nuxeo.ide.sdk.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Run a process and block until the process is terminated
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class ProcessRunner implements Runnable {

    protected ProcessBuilder builder;

    public ProcessRunner(ProcessBuilder pb) {
        pb.redirectErrorStream(true);
        this.builder = pb;
    }

    protected void output(String line) {
        // do nothing
    }

    protected void terminated(int status, Throwable e) {
        // do nothing
    }

    protected void started() {
        // do nothing
    }

    public void runAsync() {
        Thread thread = new Thread(this, "Process Runner");
        thread.start();
    }

    public void runAsJob(String jobName) {
        new Job(jobName) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("", 1);
                ProcessRunner.this.run();
                monitor.worked(1);
                monitor.done();
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @Override
    public void run() {
        started();
        Process process = null;
        try {
            process = builder.start();
            InputStream in = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    in, "UTF-8"));
            String line = reader.readLine();
            while (line != null) {
                output(line + "\n");
                line = reader.readLine();
            }
        } catch (Throwable t) {
            if (process != null) {
                process.destroy();
            }
            terminated(-1, t);
            return;
        }
        int status = 0;
        try {
            status = process.waitFor();
            closeStream(process);
            process.destroy();
        } catch (InterruptedException e) {
            closeStream(process);
            process.destroy();
            try {
                status = process.exitValue();
            } catch (IllegalThreadStateException ee) {
                // do nothing
            }
        } finally {
            // clear interrupt flag see:
            // http://bugs.sun.com/view_bug.do?bug_id=6420270
            Thread.interrupted();
            terminated(status, null);
        }
    }

    protected void closeStream(Process process) {
        try {
            process.getInputStream().close();
        } catch (Throwable t) {
            // do nothing
        }
    }
}