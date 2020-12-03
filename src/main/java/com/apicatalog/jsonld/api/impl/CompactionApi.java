/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apicatalog.jsonld.api.impl;

import java.net.URI;

import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.api.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.lang.Version;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.processor.CompactionProcessor;
import com.apicatalog.jsonld.uri.UriUtils;

import jakarta.json.JsonObject;

public final class CompactionApi implements CommonApi<CompactionApi>, LoaderApi<CompactionApi> {

    // required
    private final Document document;
    private final URI documentUri;
    private final Document context;
    private final URI contextUri;
    
    // optional
    private JsonLdOptions options;
    
    public CompactionApi(URI documentUri, Document context) {
        this.document = null;
        this.documentUri = documentUri;
        this.context = context;
        this.contextUri = null;
        this.options = new JsonLdOptions();
    }

    public CompactionApi(URI documentUri, URI contextUri) {
        this.document = null;
        this.documentUri = documentUri;
        this.context = null;
        this.contextUri = contextUri;
        this.options = new JsonLdOptions();
    }

    public CompactionApi(Document document, Document context) {
        this.document = document;
        this.documentUri = null;
        this.context = context;
        this.contextUri = null;
        this.options = new JsonLdOptions();
    }

    @Override
    public CompactionApi options(JsonLdOptions options) {
        
        if (options == null) {
            throw new IllegalArgumentException("Parameter 'options' is null.");
        }
        
        this.options = options;
        return this;
    }

    @Override
    public CompactionApi mode(Version processingMode) {
        options.setProcessingMode(processingMode);
        return this;
    }

    @Override
    public CompactionApi base(URI baseUri) {        
        options.setBase(baseUri);
        return this;
    }

    @Override
    public CompactionApi base(String baseUri) {
        return base(baseUri != null ? UriUtils.create(baseUri) : null);
    }

    
    /**
     * If set to <code>true</code>, the processor replaces arrays with just one
     * element  If set to false, all arrays will remain arrays even if they have just one
     * element. <code>true</code> by default.
     *
     * @param enable 
     * @return builder instance
     */
    public CompactionApi compactArrays(boolean enable) {
        options.setCompactArrays(enable);
        return this;
    }

    /**
     * The processor replaces arrays with just one element. 
     * 
     * @return {@link CompactionApi} instance
     */
    public CompactionApi compactArrays() {
        return compactArrays(true);
    }
    
    /**
     * Determines if IRIs are compacted relative to the {@link #base(URI)} or document location . 
     * <code>true</code> by default.
     * 
     * @param enable
     * @return builder instance
     */
    public CompactionApi compactToRelative(boolean enable) {
        options.setCompactToRelative(enable);
        return this;
    }

    /**
     * IRIs are compacted relative to the {@link #base(URI)} or document location. 
     * 
     * @return builder instance
     */
    public CompactionApi compactToRelative() {
        return compactToRelative(true);
    }
    
    @Override
    public CompactionApi loader(DocumentLoader loader) {        
        options.setDocumentLoader(loader);
        return this;
    }
    
    @Override
    public CompactionApi ordered(boolean enable) {
        options.setOrdered(enable);
        return this;
    }

    /**
     * Get the result of compaction.
     * 
     * @return {@link JsonObject} representing compacted document
     * @throws JsonLdError
     */
    public JsonObject get() throws JsonLdError {
        if (documentUri != null && contextUri != null)  {
            return CompactionProcessor.compact(documentUri, contextUri, options);
        }        
        if (documentUri != null && context != null)  {
            return CompactionProcessor.compact(documentUri, context, options);
        }
        if (document != null && context != null)  {
            return CompactionProcessor.compact(document, context, options);
        }
        throw new IllegalStateException();
    }
}
