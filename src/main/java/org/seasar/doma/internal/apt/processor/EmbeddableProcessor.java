/*
 * Copyright 2004-2010 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.doma.internal.apt.processor;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import java.io.IOException;
import java.util.Formatter;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;

import org.seasar.doma.Embeddable;
import org.seasar.doma.internal.apt.Options;
import org.seasar.doma.internal.apt.codespec.CodeSpec;
import org.seasar.doma.internal.apt.generator.EmbeddableDescGenerator;
import org.seasar.doma.internal.apt.generator.Generator;
import org.seasar.doma.internal.apt.meta.entity.EmbeddableMeta;
import org.seasar.doma.internal.apt.meta.entity.EmbeddableMetaFactory;

/**
 * @author nakamura-to
 *
 */
@SupportedAnnotationTypes({ "org.seasar.doma.Embeddable" })
@SupportedOptions({ Options.VERSION_VALIDATION, Options.RESOURCES_DIR, Options.LOMBOK_VALUE,
        Options.LOMBOK_ALL_ARGS_CONSTRUCTOR, Options.TEST, Options.DEBUG })
public class EmbeddableProcessor extends AbstractGeneratingProcessor<EmbeddableMeta> {

    public EmbeddableProcessor() {
        super(Embeddable.class);
    }

    @Override
    protected EmbeddableMetaFactory createTypeElementMetaFactory(TypeElement typeElement) {
        return new EmbeddableMetaFactory(ctx, typeElement);
    }

    @Override
    protected CodeSpec createCodeSpec(EmbeddableMeta meta) {
        return ctx.getCodeSpecs().newEmbeddableDescCodeSpec(meta.getEmbeddableElement());
    }

    @Override
    protected Generator createGenerator(EmbeddableMeta meta, CodeSpec codeSpec, Formatter formatter)
            throws IOException {
        assertNotNull(meta, codeSpec, formatter);
        return new EmbeddableDescGenerator(ctx, meta, codeSpec, formatter);
    }

}