/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.javascript.tree.impl.declaration;

import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.javascript.tree.symbols.Scope;
import org.sonar.plugins.javascript.api.tree.declaration.FunctionTree;

public abstract class FunctionTreeImpl extends JavaScriptTree implements FunctionTree {

  private Scope scope;

  @Override
  public final Scope scope() {
    return scope;
  }

  public final void scope(Scope scope) {
    this.scope = scope;
  }

}
