/*
 * Copyright (C) 2019 Saxon State and University Library Dresden (SLUB)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.qucosa.zipdisseminator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

class FilenameFilterConfiguration {

    static final FilenameFilterConfiguration EMPTY = new FilenameFilterConfiguration();
    private final LinkedHashMap<String, String> extensions = new LinkedHashMap<>();

    private final FileFilter filter = new FileFilter() {
        private LinkedHashSet<String> rejections = new LinkedHashSet<>();

        @Override
        public void reject(String mimetype, String filename) {
            rejections.add(mimetype.toLowerCase() + "::" + filename);
        }

        @Override
        public boolean accepts(String mimeType, String filename) {
            return !rejections.contains(mimeType.toLowerCase() + "::" + filename);
        }
    };
    private final LinkedHashMap<String, String> replacements = new LinkedHashMap<>();

    LinkedHashMap<String, String> extensions() {
        return extensions;
    }

    FilenameFilterConfiguration reject(String mimetype, String filename) {
        filter.reject(mimetype, filename);
        return this;
    }

    FilenameFilterConfiguration appendMissingFileExtension(String mimetype, String extension) {
        extensions.put(mimetype, extension);
        return this;
    }

    Map<String, String> replacements() {
        return Collections.unmodifiableMap(replacements);
    }

    FilenameFilterConfiguration replaceAll(String regexp, String replace) {
        replacements.put(regexp, replace);
        return this;
    }

    FileFilter filter() {
        return filter;
    }

}
