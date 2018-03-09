package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.apache.commons.lang3.Range;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class TokenizerTest extends AlexandriaBaseStoreTest {
  @Test
  public void testTokenizeText() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("a b c");
    assertThat(textTokens).hasSize(3);
  }

  @Test
  public void testTokenizeText1() {
    List<TextToken> textTokens = Tokenizer.tokenizeText(" a b c");
    assertThat(textTokens).hasSize(3);
    assertThat(textTokens.get(0).content).isEqualTo(" a ");
  }

  @Test
  public void testTokenizeText2() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("\n");
    assertThat(textTokens).hasSize(1);
    assertThat(textTokens.get(0).content).isEqualTo("\n");
  }

  @Test
  public void testTokenizeText3() {
    List<TextToken> textTokens = Tokenizer.tokenizeText(" ");
    assertThat(textTokens).hasSize(1);
    assertThat(textTokens.get(0).content).isEqualTo(" ");
  }

  @Test
  public void testTokenizeText4() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("! ");
    assertThat(textTokens).hasSize(1);
    assertThat(textTokens.get(0).content).isEqualTo("! ");
  }

  @Test
  public void testTokenizeText5() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("Alas, poor Yorick!");
    assertThat(textTokens).hasSize(3);
    assertThat(textTokens.get(0).content).isEqualTo("Alas, ");
    assertThat(textTokens.get(1).content).isEqualTo("poor ");
    assertThat(textTokens.get(2).content).isEqualTo("Yorick!");
  }

  @Test
  public void testTokenizeText6() {
    List<TextToken> textTokens = Tokenizer.tokenizeText("Lucy, for you the snowdrop and the bay");
    assertThat(textTokens).hasSize(8);
    assertThat(textTokens.get(0).content).isEqualTo("Lucy, ");
    assertThat(textTokens.get(1).content).isEqualTo("for ");
    assertThat(textTokens.get(2).content).isEqualTo("you ");
  }

  @Test
  public void testTokenizer() {
    store.runInTransaction(() -> {
      DocumentWrapper doc = new LMNLImporter(store).importLMNL("[l}[phr}Alas,{phr] [phr}poor Yorick!{phr]{l]");
      TAGView onlyLines = new TAGView(store).setMarkupToInclude(singleton("l"));
      Tokenizer tokenizer = new Tokenizer(doc, onlyLines);
      List<TAGToken> tokens = tokenizer.getTAGTokens();
      assertThat(tokens).extracting("content")//
          .containsExactly("l", "Alas, ", "poor ", "Yorick!", "l");

      Map<TAGToken, List<TokenProvenance>> tokenProvenanceMap = tokenizer.getTokenProvenanceMap();

      // [l}
      TAGToken tagToken0 = tokens.get(0);
      List<TokenProvenance> tokenProvenances0 = tokenProvenanceMap.get(tagToken0);
      assertThat(tokenProvenances0).hasSize(1);

      assertMarkupOpenTokenProvenance(tokenProvenances0.get(0), "l", "Alas,");

      // "Alas, "
      TAGToken tagToken1 = tokens.get(1);
      List<TokenProvenance> tokenProvenances1 = tokenProvenanceMap.get(tagToken1);
      assertThat(tokenProvenances1).hasSize(2);

      assertTextTokenProvenance(tokenProvenances1.get(0), 0, "Alas,");
      assertTextTokenProvenance(tokenProvenances1.get(1), 0, " ");

      // {l]
      TAGToken tagToken4 = tokens.get(4);
      List<TokenProvenance> tokenProvenances4 = tokenProvenanceMap.get(tagToken4);
      assertThat(tokenProvenances4).hasSize(1);

      assertMarkupCloseTokenProvenance(tokenProvenances4.get(0), "l", "poor Yorick!");
    });
  }

  private void assertMarkupOpenTokenProvenance(final TokenProvenance tokenProvenance, final String expectedTag, final String expectedTextNodeText) {
    assertThat(tokenProvenance).isInstanceOf(MarkupOpenTokenProvenance.class);

    MarkupOpenTokenProvenance markupOpenTokenProvenance = (MarkupOpenTokenProvenance) tokenProvenance;
    assertThat(markupOpenTokenProvenance.getTag()).isEqualTo(expectedTag);
    assertThat(markupOpenTokenProvenance.getTextNodeWrapper().getText()).isEqualTo(expectedTextNodeText);
  }

  private void assertTextTokenProvenance(final TokenProvenance tokenProvenance, final int expectedOffset, final String expectedContent) {
    assertThat(tokenProvenance).isInstanceOf(TextTokenProvenance.class);

    TextTokenProvenance textTokenProvenance11 = (TextTokenProvenance) tokenProvenance;
    assertThat(textTokenProvenance11.getOffset()).isEqualTo(expectedOffset);
    assertThat(textTokenProvenance11.getTextNodeWrapper().getText()).isEqualTo(expectedContent);
  }

  private void assertMarkupCloseTokenProvenance(final TokenProvenance tokenProvenance, final String expectedTag, final String expectedTextNodeText) {
    assertThat(tokenProvenance).isInstanceOf(MarkupCloseTokenProvenance.class);

    MarkupCloseTokenProvenance markupCloseTokenProvenance = (MarkupCloseTokenProvenance) tokenProvenance;
    assertThat(markupCloseTokenProvenance.getTag()).isEqualTo(expectedTag);
    assertThat(markupCloseTokenProvenance.getTextNodeWrapper().getText()).isEqualTo(expectedTextNodeText);
  }

  @Test
  public void testCalcRange() {
    String text = "1234567890";
    AtomicInteger offset = new AtomicInteger(0);
    Range<Integer> range = Tokenizer.calcRange(text, offset);
    softly.assertThat(range.getMinimum()).isEqualTo(0);
    softly.assertThat(range.getMaximum()).isEqualTo(9);
    softly.assertThat(offset.get()).isEqualTo(10);

    Range<Integer> otherRange = Range.between(8, 16);
    softly.assertThat(range.isOverlappedBy(otherRange)).isTrue();
    softly.assertThat(otherRange.isOverlappedBy(range)).isTrue();
  }
}
