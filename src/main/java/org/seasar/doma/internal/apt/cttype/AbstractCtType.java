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
package org.seasar.doma.internal.apt.cttype;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.seasar.doma.internal.apt.AptIllegalStateException;
import org.seasar.doma.internal.apt.Context;

/**
 * @author taedium
 * 
 */
public abstract class AbstractCtType implements CtType {

    protected final Context ctx;

    protected final TypeMirror type;

    protected final String typeName;

    protected final TypeElement typeElement;

    protected final String qualifiedName;

    protected final boolean isRawType;

    protected final boolean hasWildcardType;

    protected final boolean hasTypevarType;

    AbstractCtType(Context ctx, TypeMirror type) {
        assertNotNull(ctx, type);
        this.ctx = ctx;
        this.type = type;
        this.typeName = ctx.getTypes().getTypeName(type);
        this.typeElement = ctx.getTypes().toTypeElement(type);
        if (typeElement == null) {
            this.qualifiedName = typeName;
        } else {
            this.qualifiedName = typeElement.getQualifiedName().toString();
        }
        this.isRawType = isRawType(ctx, type, typeElement);
        if (isRawType) {
            this.hasWildcardType = false;
            this.hasTypevarType = false;
        } else {
            DeclaredType declaredType = ctx.getTypes().toDeclaredType(type);
            this.hasWildcardType = matchTypeArguments(declaredType, TypeKind.WILDCARD);
            this.hasTypevarType = matchTypeArguments(declaredType, TypeKind.TYPEVAR);
        }
    }

    private static boolean isRawType(Context ctx, TypeMirror type, TypeElement typeElement) {
        if (typeElement != null && !typeElement.getTypeParameters().isEmpty()) {
            DeclaredType declaredType = ctx.getTypes().toDeclaredType(type);
            if (declaredType == null) {
                throw new AptIllegalStateException(type.toString());
            }
            if (declaredType.getTypeArguments().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchTypeArguments(DeclaredType declaredType, TypeKind kind) {
        if (declaredType == null) {
            return false;
        }
        return declaredType.getTypeArguments().stream().anyMatch(a -> a.getKind() == kind);
    }

    @Override
    public TypeMirror getType() {
        return type;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public boolean isRawType() {
        return isRawType;
    }

    @Override
    public boolean hasWildcardType() {
        return hasWildcardType;
    }

    @Override
    public boolean hasTypevarType() {
        return hasTypevarType;
    }

}
