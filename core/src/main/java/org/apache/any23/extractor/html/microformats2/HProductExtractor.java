/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.any23.extractor.html.microformats2;

import org.apache.any23.extractor.ExtractionException;
import org.apache.any23.extractor.ExtractionResult;
import org.apache.any23.extractor.ExtractorDescription;
import org.apache.any23.extractor.html.EntityBasedMicroformatExtractor;
import org.apache.any23.extractor.html.HTMLDocument;
import org.apache.any23.vocab.HProduct;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Extractor for the <a href="http://microformats.org/wiki/h-product">h-product</a>
 * microformat.
 *
 * @author Nisala Nirmana
 */
public class HProductExtractor extends EntityBasedMicroformatExtractor {

    private static final HProduct vProduct = HProduct.getInstance();

    private static final String[] productFields = {
            "name",
            "photo",
            "brand",
            "category",
            "description",
            "url",
            "identifier",
            "review", //toDo
            "price"
    };

    @Override
    public ExtractorDescription getDescription() {
        return HProductExtractorFactory.getDescriptionInstance();
    }

    @Override
    protected String getBaseClassName() {
        return Microformats2Prefixes.CLASS_PREFIX+"product";
    }

    @Override
    protected void resetExtractor() {
        // Empty.
    }

    @Override
    protected boolean extractEntity(Node node, ExtractionResult out) throws ExtractionException {
        final BNode product = getBlankNodeFor(node);
        conditionallyAddResourceProperty(product, RDF.TYPE, vProduct.product);
        final HTMLDocument fragment = new HTMLDocument(node);
        addName(fragment, product);
        addPhoto(fragment, product);
        addCategories(fragment, product);
        addDescription(fragment, product);
        addURLs(fragment, product);
        addIdentifiers(fragment, product);
        addPrice(fragment, product);
        addBrand(fragment,product);
        return true;
    }

    private void mapFieldWithProperty(HTMLDocument fragment, BNode product, String fieldClass,
                                      URI property) {
        HTMLDocument.TextField title = fragment.getSingularTextField(fieldClass);
        conditionallyAddStringProperty(
                title.source(), product, property, title.value()
        );
    }

    private void addName(HTMLDocument fragment, BNode product) {
        mapFieldWithProperty(fragment, product, Microformats2Prefixes.PROPERTY_PREFIX +
                productFields[0], vProduct.name);
    }

    private void addPhoto(HTMLDocument fragment, BNode product) throws ExtractionException {
        final HTMLDocument.TextField[] photos = fragment.getPluralUrlField
                (Microformats2Prefixes.URL_PROPERTY_PREFIX + productFields[1]);
        for(HTMLDocument.TextField photo : photos) {
            addURIProperty(product, vProduct.photo, fragment.resolveURI(photo.value()));
        }
    }

    private void addCategories(HTMLDocument fragment, BNode product) {
        final HTMLDocument.TextField[] categories = fragment.getPluralTextField
                (Microformats2Prefixes.PROPERTY_PREFIX + productFields[3]);
        for(HTMLDocument.TextField category : categories) {
            conditionallyAddStringProperty(
                    category.source(), product, vProduct.category, category.value()
            );
        }
    }

    private void addDescription(HTMLDocument fragment, BNode product) {
        mapFieldWithProperty(fragment, product, Microformats2Prefixes.EMBEDDED_PROPERTY_PREFIX +
                productFields[4], vProduct.description);
    }

    private void addURLs(HTMLDocument fragment, BNode product) throws ExtractionException {
        final HTMLDocument.TextField[] urls = fragment.getPluralUrlField
                (Microformats2Prefixes.URL_PROPERTY_PREFIX + productFields[5]);
        for(HTMLDocument.TextField url : urls) {
            addURIProperty(product, vProduct.url, fragment.resolveURI(url.value()));
        }
    }

    private void addIdentifiers(HTMLDocument fragment, BNode product) throws ExtractionException {
        final HTMLDocument.TextField[] identifiers = fragment.getPluralUrlField
                (Microformats2Prefixes.URL_PROPERTY_PREFIX + productFields[6]);
        for(HTMLDocument.TextField identifier :identifiers) {
            addURIProperty(product, vProduct.identifier, fragment.resolveURI(identifier.value()));
        }
    }

    private void addPrice(HTMLDocument fragment, BNode product) {
        final HTMLDocument.TextField price = fragment.getSingularTextField(
                Microformats2Prefixes.PROPERTY_PREFIX + productFields[8]);
        if(price.source()==null)
            return;
        Node attribute = price.source().getAttributes().getNamedItem("value");
        if (attribute == null) {
            conditionallyAddStringProperty(
                    price.source(),
                    product, vProduct.price, price.value()
            );
        } else {
            conditionallyAddStringProperty(
                    price.source(),
                    product, vProduct.price, attribute.getNodeValue()
            );
        }
    }

    private void addBrand(HTMLDocument doc, Resource product) throws ExtractionException {
        List<Node> nodes = doc.findAllByClassName(Microformats2Prefixes.PROPERTY_PREFIX + productFields[2] +
                Microformats2Prefixes.SPACE_SEPARATOR + Microformats2Prefixes.CLASS_PREFIX + "card");
        if (nodes.isEmpty())
            return;
        HCardExtractorFactory factory = new HCardExtractorFactory();
        HCardExtractor extractor = factory.createExtractor();
        for (Node node : nodes) {
            BNode brand = valueFactory.createBNode();
            addURIProperty(brand, RDF.TYPE, vProduct.brand);
            extractor.extractEntityAsEmbeddedProperty(new HTMLDocument(node), brand,
                    getCurrentExtractionResult());
        }
    }
}
