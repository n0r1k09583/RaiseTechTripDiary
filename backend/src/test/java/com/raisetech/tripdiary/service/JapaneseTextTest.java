package com.raisetech.tripdiary.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JapaneseTextTest {

  @Test
  void ひらがなとカタカナを相互に変換する() {
    assertThat(JapaneseText.toKatakana("やまだ")).isEqualTo("ヤマダ");
    assertThat(JapaneseText.toHiragana("ハナコ")).isEqualTo("はなこ");
  }

  @Test
  void かなをローマ字にする() {
    assertThat(JapaneseText.toRomaji("やまだ")).isEqualTo("yamada");
    assertThat(JapaneseText.toRomaji("ヤマダ")).isEqualTo("yamada");
    assertThat(JapaneseText.toRomaji("はなこ")).isEqualTo("hanako");
    assertThat(JapaneseText.toRomaji("いちろう")).isEqualTo("ichirou");
    assertThat(JapaneseText.compactRomaji("ichirou")).isEqualTo("ichiro");
  }
}
