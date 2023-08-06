/**
 * Kalbum
 * <p>
 * Copyright 2023 Kenny Colliander
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.kecon.kalbum;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * POJO for an album.
 *
 * @author Kenny Colliander
 * @since 2023-07-31
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Album extends AlbumContent implements Cloneable {

    private List<ContentData> contents;


    @Override
    public Album clone() {
        Album clone = (Album) super.clone();

        if (this.contents != null) {
            clone.setContents(this.contents.stream().map(ContentData::clone).collect(Collectors.toList()));
        }

        return clone;
    }
}
