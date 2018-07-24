package nl.knaw.huygens.alexandria.storage;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import nl.knaw.huygens.alexandria.storage.dto.TAGDTO;

@Persistent
public class AnnotationValue implements TAGDTO {
  @PrimaryKey(sequence = "annotation_pk_sequence")
  private Long id;

  public AnnotationValue() {
  }

  @Override
  public Long getDbId() {
    return id;
  }

}