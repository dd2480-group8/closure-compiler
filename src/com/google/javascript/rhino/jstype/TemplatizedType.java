/*
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Bob Jervis
 *   Google Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package com.google.javascript.rhino.jstype;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.javascript.rhino.ErrorReporter;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Node;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * An object type with declared template types, such as
 * {@code Array<string>}.
 *
 */
public final class TemplatizedType extends ObjectType {
  private static final long serialVersionUID = 1L;

  private JSType referencedType;
  private ObjectType referencedObjType;

  /** A cache of the type parameter values for this specialization. */
  private final ImmutableList<JSType> templateTypes;
  /** Whether all type parameter values for this specialization are `?`. */
  private final boolean isSpecializedOnlyWithUnknown;

  private transient TemplateTypeMapReplacer replacer;

  TemplatizedType(
      JSTypeRegistry registry, ObjectType objectType,
      ImmutableList<JSType> templateTypes) {
    super(
        registry, objectType.getTemplateTypeMap().copyFilledWithValues(templateTypes));
    
    setReferencedType(checkNotNull(objectType));

    ImmutableList.Builder<JSType> builder = ImmutableList.builder();
    boolean maybeIsSpecializedOnlyWithUnknown = true;
    for (TemplateType newlyFilledTemplateKey :
        objectType.getTemplateTypeMap().getUnfilledTemplateKeys()) {
      JSType resolvedType = getTemplateTypeMap().getResolvedTemplateType(newlyFilledTemplateKey);

      builder.add(resolvedType);
      maybeIsSpecializedOnlyWithUnknown =
          maybeIsSpecializedOnlyWithUnknown && resolvedType.isUnknownType();
    }
    this.templateTypes = builder.build();
    this.isSpecializedOnlyWithUnknown = maybeIsSpecializedOnlyWithUnknown;

    this.replacer = new TemplateTypeMapReplacer(registry, getTemplateTypeMap());
  }

  @Override
  public final HasPropertyKind getPropertyKind(String propertyName, boolean autobox) {
    return referencedType.getPropertyKind(propertyName, autobox);
  }

  @Override
  final PropertyMap getPropertyMap() {
    return referencedObjType == null
        ? PropertyMap.immutableEmptyMap() : referencedObjType.getPropertyMap();
  }

  final JSType getReferencedTypeInternal() {
    return referencedType;
  }

  final ObjectType getReferencedObjTypeInternal() {
    return referencedObjType;
  }

  final void setReferencedType(JSType referencedType) {
    this.referencedType = referencedType;
    if (referencedType instanceof ObjectType) {
      this.referencedObjType = (ObjectType) referencedType;
    } else {
      this.referencedObjType = null;
    }
  }

  @Override
  public boolean setValidator(Predicate<JSType> validator) {
    // The referenced type might have specialized behavior for validation, e.g. {@link NamedType}
    // defers validation until after named type resolution.
    return referencedType.setValidator(validator);
  }

  @Override
  public String getReferenceName() {
    return referencedObjType == null ? "" : referencedObjType.getReferenceName();
  }

  @Override
  public final boolean matchesNumberContext() {
    return referencedType.matchesNumberContext();
  }

  @Override
  public final boolean matchesStringContext() {
    return referencedType.matchesStringContext();
  }

  @Override
  public final boolean matchesSymbolContext() {
    return referencedType.matchesSymbolContext();
  }

  @Override
  public final boolean matchesObjectContext() {
    return referencedType.matchesObjectContext();
  }

  @Override
  public final boolean canBeCalled() {
    return referencedType.canBeCalled();
  }

  @Override
  public final boolean isStructuralType() {
    return referencedType.isStructuralType();
  }

  @Override
  public final boolean isNoType() {
    return referencedType.isNoType();
  }

  @Override
  public final boolean isNoObjectType() {
    return referencedType.isNoObjectType();
  }

  @Override
  public final boolean isNoResolvedType() {
    return referencedType.isNoResolvedType();
  }

  @Override
  public final boolean isUnknownType() {
    return referencedType.isUnknownType();
  }

  @Override
  public final boolean isCheckedUnknownType() {
    return referencedType.isCheckedUnknownType();
  }

  @Override
  public final boolean isNullable() {
    return referencedType.isNullable();
  }

  @Override
  public final boolean isVoidable() {
    return referencedType.isVoidable();
  }

  @Override
  public final EnumType toMaybeEnumType() {
    return referencedType.toMaybeEnumType();
  }

  @Override
  public final boolean isConstructor() {
    return referencedType.isConstructor();
  }

  @Override
  public boolean isNominalType() {
    return referencedType.isNominalType();
  }

  @Override
  public final boolean isInstanceType() {
    return referencedType.isInstanceType();
  }

  @Override
  public final boolean isInterface() {
    return referencedType.isInterface();
  }

  @Override
  public final boolean isOrdinaryFunction() {
    return referencedType.isOrdinaryFunction();
  }

  @Override
  public final boolean isAllType() {
    return referencedType.isAllType();
  }

  @Override
  public final boolean isStruct() {
    return referencedType.isStruct();
  }

  @Override
  public final boolean isDict() {
    return referencedType.isDict();
  }

  @Override
  public final boolean isNativeObjectType() {
    return referencedObjType == null
        ? false : referencedObjType.isNativeObjectType();
  }

  @Override
  public final RecordType toMaybeRecordType() {
    return referencedType.toMaybeRecordType();
  }

  @Override
  public final UnionType toMaybeUnionType() {
    return referencedType.toMaybeUnionType();
  }

  @Override
  public final FunctionType toMaybeFunctionType() {
    return referencedType.toMaybeFunctionType();
  }

  @Override
  public final EnumElementType toMaybeEnumElementType() {
    return referencedType.toMaybeEnumElementType();
  }

  @Override
  public final TernaryValue testForEquality(JSType that) {
    return referencedType.testForEquality(that);
  }

  @Override
  public final FunctionType getOwnerFunction() {
    return referencedObjType == null
        ? null : referencedObjType.getOwnerFunction();
  }

  @Override
  public final ObjectType getImplicitPrototype() {
    return referencedObjType == null ? null :
        referencedObjType.getImplicitPrototype();
  }

  @Override
  boolean defineProperty(String propertyName, JSType type, boolean inferred, Node propertyNode) {
    return referencedObjType == null
        || referencedObjType.defineProperty(propertyName, type, inferred, propertyNode);
  }

  @Override
  public final boolean removeProperty(String name) {
    return referencedObjType == null ? false :
        referencedObjType.removeProperty(name);
  }

  @Override
  protected JSType findPropertyTypeWithoutConsideringTemplateTypes(String propertyName) {
    return referencedType.findPropertyType(propertyName);
  }

  @Override
  public final JSDocInfo getJSDocInfo() {
    return referencedType.getJSDocInfo();
  }

  @Override
  public final void setJSDocInfo(JSDocInfo info) {
    if (referencedObjType != null) {
      referencedObjType.setJSDocInfo(info);
    }
  }

  @Override
  public final void setPropertyJSDocInfo(String propertyName, JSDocInfo info) {
    if (referencedObjType != null) {
      referencedObjType.setPropertyJSDocInfo(propertyName, info);
    }
  }

  @Override
  public final FunctionType getConstructor() {
    return referencedObjType == null ? null :
        referencedObjType.getConstructor();
  }

  public final <T> T visitReferenceType(Visitor<T> visitor) {
    return referencedType.visit(visitor);
  }
  
  @Override
  JSType resolveInternal(ErrorReporter reporter) {
    setReferencedType(referencedType.resolve(reporter));
    return this;
  }

  @Override
  public final String toDebugHashCodeString() {
    return "{proxy:" + referencedType.toDebugHashCodeString() + "}";
  }

  @Override
  public final JSType getTypeOfThis() {
    if (referencedObjType != null) {
      return referencedObjType.getTypeOfThis();
    }
    return super.getTypeOfThis();
  }

  @Override
  public final JSType collapseUnion() {
    if (referencedType.isUnionType()) {
      return referencedType.collapseUnion();
    }
    return this;
  }

  @Override
  public final void matchConstraint(JSType constraint) {
    referencedType.matchConstraint(constraint);
  }

  @Override
  public TemplateType toMaybeTemplateType() {
    return referencedType.toMaybeTemplateType();
  }

  @Override
  public Iterable<ObjectType> getCtorImplementedInterfaces() {
    LinkedHashSet<ObjectType> resolvedImplementedInterfaces = new LinkedHashSet<>();
    for (ObjectType obj : getReferencedObjTypeInternal().getCtorImplementedInterfaces()) {
      resolvedImplementedInterfaces.add(obj.visit(replacer).toObjectType());
    }
    return resolvedImplementedInterfaces;
  }

  @Override
  public Iterable<ObjectType> getCtorExtendedInterfaces() {
    LinkedHashSet<ObjectType> resolvedExtendedInterfaces = new LinkedHashSet<>();
    for (ObjectType obj : getReferencedObjTypeInternal().getCtorExtendedInterfaces()) {
      resolvedExtendedInterfaces.add(obj.visit(replacer).toObjectType());
    }
    return resolvedExtendedInterfaces;
  }

  @Override
  StringBuilder appendTo(StringBuilder sb, boolean forAnnotations) {
    referencedType.appendTo(sb, forAnnotations);

    if (!this.templateTypes.isEmpty()) {
      sb.append("<");
      int lastIndex = this.templateTypes.size() - 1;
      for (int i = 0; i < lastIndex; i++) {
        this.templateTypes.get(i).appendTo(sb, forAnnotations);
        sb.append(",");
      }
      this.templateTypes.get(lastIndex).appendTo(sb, forAnnotations);
      sb.append(">");
    }
    return sb;
  }

  @Override
  int recursionUnsafeHashCode() {
    int baseHash = referencedType.hashCode();

    // TODO(b/110224889): This case can probably be removed if `equals()` is updated.
    if (isSpecializedOnlyWithUnknown) {
      return baseHash;
    }
    return Objects.hash(templateTypes, baseHash);
  }

  @Override
  public <T> T visit(Visitor<T> visitor) {
    return visitor.caseTemplatizedType(this);
  }

  @Override <T> T visit(RelationshipVisitor<T> visitor, JSType that) {
    return visitor.caseTemplatizedType(this, that);
  }

  @Override
  public TemplatizedType toMaybeTemplatizedType() {
    return this;
  }

  @Override
  public ImmutableList<JSType> getTemplateTypes() {
    return templateTypes;
  }

  @Override
  public JSType getPropertyType(String propertyName) {
    JSType result = super.getPropertyType(propertyName);
    return result == null ? null : result.visit(replacer);
  }

  @Override
  public boolean isSubtype(JSType that) {
    return isSubtype(that, ImplCache.create(), SubtypingMode.NORMAL);
  }

  @Override
  protected boolean isSubtype(JSType that,
      ImplCache implicitImplCache, SubtypingMode subtypingMode) {
    return isSubtypeHelper(this, that, implicitImplCache, subtypingMode);
  }

  boolean wrapsSameRawType(JSType that) {
    return that.isTemplatizedType() && this.getReferencedTypeInternal()
        .isEquivalentTo(
            that.toMaybeTemplatizedType().getReferencedTypeInternal());
  }

  /**
   * Computes the greatest subtype of two related templatized types.
   * @return The greatest subtype.
   */
  JSType getGreatestSubtypeHelper(JSType rawThat) {
    checkNotNull(rawThat);

    if (!wrapsSameRawType(rawThat)) {
      if (!rawThat.isTemplatizedType()) {
        if (this.isSubtype(rawThat)) {
          return this;
        } else if (rawThat.isSubtypeOf(this)) {
          return filterNoResolvedType(rawThat);
        }
      }
      if (this.isObject() && rawThat.isObject()) {
        return this.getNativeType(JSTypeNative.NO_OBJECT_TYPE);
      }
      return this.getNativeType(JSTypeNative.NO_TYPE);
    }

    TemplatizedType that = rawThat.toMaybeTemplatizedType();
    checkNotNull(that);

    if (getTemplateTypeMap().checkEquivalenceHelper(
        that.getTemplateTypeMap(), EquivalenceMethod.INVARIANT, SubtypingMode.NORMAL)) {
      return this;
    }

    // For types that have the same raw type but different type parameters,
    // we simply create a type has a "unknown" type parameter.  This is
    // equivalent to the raw type.
    return getReferencedObjTypeInternal();
  }

  @Override
  public TemplateTypeMap getTemplateTypeMap() {
    return templateTypeMap;
  }

  @Override
  public boolean hasAnyTemplateTypesInternal() {
    return templateTypeMap.hasAnyTemplateTypesInternal();
  }

  /**
   * @return The referenced ObjectType wrapped by this TemplatizedType.
   */
  public ObjectType getReferencedType() {
    return getReferencedObjTypeInternal();
  }

  @GwtIncompatible("ObjectInputStream")
  private void readObject(java.io.ObjectInputStream in) throws Exception {
    in.defaultReadObject();
    replacer = new TemplateTypeMapReplacer(registry, templateTypeMap);
  }
}
