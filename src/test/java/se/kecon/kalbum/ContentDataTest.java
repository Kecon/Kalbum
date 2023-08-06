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

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test {@link ContentData}
 * <p>
 * Created 2023-08-06 by Kenny Colliander
 */
class ContentDataTest {

    @Test
    void testCopyConstructor() {
        ContentData contentData = new ContentData();
        contentData.setContentType("contentType");
        contentData.setSrc("src");
        contentData.setAlt("alt");
        contentData.setText("text");
        contentData.setWidth(1);
        contentData.setHeight(2);
        contentData.setTimestamp(Instant.now());

        ContentData contentDataCopy = new ContentData(contentData);
        assertEquals(contentData.getContentType(), contentDataCopy.getContentType());
        assertEquals(contentData.getSrc(), contentDataCopy.getSrc());
        assertEquals(contentData.getAlt(), contentDataCopy.getAlt());
        assertEquals(contentData.getText(), contentDataCopy.getText());
        assertEquals(contentData.getWidth(), contentDataCopy.getWidth());
        assertEquals(contentData.getHeight(), contentDataCopy.getHeight());
        assertEquals(contentData.getTimestamp(), contentDataCopy.getTimestamp());
    }
}