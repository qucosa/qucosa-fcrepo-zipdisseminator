/*
 * Copyright 2016 Saxon State and University Library Dresden (SLUB)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.qucosa.XmlUtils;

import org.jdom.Namespace;

import java.util.HashMap;
import java.util.Map;

public final class Namespaces {

    public static final Namespace METS = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
    public static final Namespace MEXT = Namespace.getNamespace("mext", "http://slub-dresden.de/mets");
    public static final Namespace MODS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");
    public static final Namespace SLUB = Namespace.getNamespace("slub", "http://slub-dresden.de/");
    public static final Namespace XLIN = Namespace.getNamespace("xlin", "http://www.w3.org/1999/xlink");
    public static final Namespace main = Namespace.getNamespace("main", "http://schemas.openxmlformats.org/spreadsheetml/2006/main");
    public static final Namespace r = Namespace.getNamespace("r", "http://schemas.openxmlformats.org/officeDocument/2006/relationships");

    private Namespaces() {
    }

    static public Map<String, String> getPrefixUriMap() {
        return new HashMap<String, String>() {{
            put(METS.getPrefix(), METS.getURI());
            put(MEXT.getPrefix(), MEXT.getURI());
            put(MODS.getPrefix(), MODS.getURI());
            put(SLUB.getPrefix(), SLUB.getURI());
            put(XLIN.getPrefix(), XLIN.getURI());
            put(main.getPrefix(), main.getURI());
            put(r.getPrefix(), r.getURI());
        }};
    }

}
