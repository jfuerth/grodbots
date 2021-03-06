/*
 * Created on Jul 16, 2007
 *
 * Copyright (c) 2007, Jonathan Fuerth
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Jonathan Fuerth nor the names of other
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.bluecow.robot.resource.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import net.bluecow.robot.resource.ResourceLoader;

public class ResourceURLConnection extends URLConnection {
    
    private String resourceName;
    private InputStream in;
    private ResourceLoader resourceLoader;
    
    protected ResourceURLConnection(URL url, ResourceLoader resourceLoader) {
        super(url);
        this.resourceLoader = resourceLoader;
        resourceName = url.getPath();
        if (resourceName.startsWith("/")) {
            resourceName = resourceName.substring(1);
        }
    }

    @Override
    public void connect() throws IOException {
        if (connected) return;
        in = resourceLoader.getResourceAsStream(resourceName);
        if (in == null) {
            throw new IOException("Resource not found: " + resourceName);
        }
        connected = true;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        return in;
    }

    @Override
    public String getContentType() {
        // XXX ResourceUtils should be able to guess content type for us
        //     or better yet, the resource loader would be able to say
        return "application/octet-stream";
    }
}
