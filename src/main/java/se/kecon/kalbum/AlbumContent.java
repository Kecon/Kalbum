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
import lombok.NoArgsConstructor;

/**
 * Only contain the id and name of an album. This is used to reduce the amount of data sent to the client.
 *
 * @author Kenny Colliander
 * @since 2023-07-31
 */
@Data
@NoArgsConstructor
public class AlbumContent {

    private String id;

    private String name;

    public AlbumContent(AlbumContent albumContent) {
        this.id = albumContent.getId();
        this.name = albumContent.getName();
    }
}
