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
package com.apicatalog.jsonld.processor;

import java.net.URI;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.compaction.Compaction;
import com.apicatalog.jsonld.context.ActiveContext;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.json.JsonUtils;
import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 *
 * @see <a href="https://www.w3.org/TR/json-ld11-api/#dom-jsonldprocessor-compact">JsonLdProcessor.compact()</a>
 *
 */
public final class CompactionProcessor {

    private CompactionProcessor() {
    }

    public static final JsonObject compact(final URI input, final URI context, final JsonLdOptions options) throws JsonLdError {

        if (options.getDocumentLoader() == null) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Document loader is null. Cannot fetch [" + context + "].");
        }

        final Document contextDocument = options.getDocumentLoader().loadDocument(context, new DocumentLoaderOptions());

        if (contextDocument == null) {
            throw new JsonLdError(JsonLdErrorCode.INVALID_REMOTE_CONTEXT, "Context[" + context + "] is null.");
        }

        return compact(input, contextDocument, options);
    }

    public static final JsonObject compact(final URI input, final Document context, final JsonLdOptions options) throws JsonLdError {

        if (options.getDocumentLoader() == null) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Document loader is null. Cannot fetch [" + input + "].");
        }

        final DocumentLoaderOptions loaderOptions = new DocumentLoaderOptions();
        loaderOptions.setExtractAllScripts(options.isExtractAllScripts());

        final Document remoteDocument = options.getDocumentLoader().loadDocument(input, loaderOptions);

        if (remoteDocument == null) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Returned document is null [" + input + "].");
        }

        return compact(remoteDocument, context, options);
    }

    public static final JsonObject compact(final Document input, final URI context, final JsonLdOptions options) throws JsonLdError {

        if (options.getDocumentLoader() == null) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Document loader is null. Cannot fetch [" + context + "].");
        }

        final Document contextDocument = options.getDocumentLoader().loadDocument(context, new DocumentLoaderOptions());

        if (contextDocument == null) {
            throw new JsonLdError(JsonLdErrorCode.INVALID_REMOTE_CONTEXT, "Returned context is null [" + context + "] is null.");
        }

        return compact(input, contextDocument, options);
    }

    public static final JsonObject compact(final Document input, final Document context, final JsonLdOptions options) throws JsonLdError {

        // 4.
        final JsonLdOptions expansionOptions = new JsonLdOptions(options);
        expansionOptions.setOrdered(false);
        expansionOptions.setExtractAllScripts(false);

        JsonArray expandedInput = ExpansionProcessor.expand(input, expansionOptions, false);

        // 5.
        URI contextBase = input.getDocumentUrl();

        if (contextBase == null) {
            contextBase = options.getBase();
        }

        // 6.
        final JsonValue contextValue = context.getJsonContent()
                                        .map(ctx -> JsonUtils.flatten(ctx, Keywords.CONTEXT))
                                        .orElse(JsonValue.EMPTY_JSON_OBJECT);

        // 7.
        final ActiveContext activeContext = new ActiveContext(options).newContext().create(contextValue, contextBase);

        // 8.
        if (activeContext.getBaseUri() == null) {

            if (options.getBase() != null) {
                activeContext.setBaseUri(options.getBase());

            } else if (options.isCompactToRelative()) {
                activeContext.setBaseUri(input.getDocumentUrl());
            }
        }

        // 9.
        JsonValue compactedOutput = Compaction
                                        .with(activeContext)
                                        .compactArrays(options.isCompactArrays())
                                        .ordered(options.isOrdered())
                                        .compact(expandedInput);

        // 9.1.
        if (JsonUtils.isEmptyArray(compactedOutput)) {
            compactedOutput = JsonValue.EMPTY_JSON_OBJECT;

        // 9.2.
        } else if (JsonUtils.isArray(compactedOutput)) {
            compactedOutput = Json.createObjectBuilder()
                                    .add(
                                        activeContext.uriCompaction().vocab(true).compact(Keywords.GRAPH),
                                        compactedOutput
                                        )
                                    .build();
        }

        if (JsonUtils.isNull(compactedOutput) || compactedOutput.asJsonObject().isEmpty()) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }

        // 9.3.
        if (JsonUtils.isNotNull(contextValue)
                && !JsonUtils.isEmptyArray(contextValue)
                && !JsonUtils.isEmptyObject(contextValue)
                ) {
            compactedOutput = Json.createObjectBuilder(compactedOutput.asJsonObject())
                                    .add(Keywords.CONTEXT, contextValue)
                                    .build();
        }

        return compactedOutput.asJsonObject();
    }
}
