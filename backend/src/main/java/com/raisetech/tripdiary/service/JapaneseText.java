package com.raisetech.tripdiary.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class JapaneseText {

  private static final Map<String, String> ROMAJI = romajiTable();

  private JapaneseText() {}

  static String toHiragana(String raw) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c >= 'ァ' && c <= 'ヶ') {
        out.append((char) (c - 0x60));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  static String toKatakana(String raw) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c >= 'ぁ' && c <= 'ゖ') {
        out.append((char) (c + 0x60));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  static String toRomaji(String raw) {
    String hira = toHiragana(raw).toLowerCase(Locale.ROOT);
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < hira.length()) {
      char c = hira.charAt(i);
      if (c == 'っ' && i + 1 < hira.length()) {
        String next = nextSyllable(hira, i + 1);
        if (!next.isEmpty() && isConsonant(next.charAt(0))) {
          out.append(next.charAt(0));
          i++;
          continue;
        }
      }
      if (c == 'ー' && !out.isEmpty()) {
        char last = out.charAt(out.length() - 1);
        if ("aiueo".indexOf(last) >= 0) {
          out.append(last);
        }
        i++;
        continue;
      }
      String two = i + 1 < hira.length() ? hira.substring(i, i + 2) : "";
      if (ROMAJI.containsKey(two)) {
        out.append(ROMAJI.get(two));
        i += 2;
        continue;
      }
      String one = hira.substring(i, i + 1);
      if (ROMAJI.containsKey(one)) {
        out.append(ROMAJI.get(one));
      } else {
        out.append(one);
      }
      i++;
    }
    return out.toString();
  }

  static String compactRomaji(String roma) {
    return roma.replace("ou", "o").replace("uu", "u").replace("oo", "o");
  }

  private static String nextSyllable(String hira, int index) {
    String two = index + 1 < hira.length() ? hira.substring(index, index + 2) : "";
    if (ROMAJI.containsKey(two)) {
      return ROMAJI.get(two);
    }
    String one = hira.substring(index, index + 1);
    return ROMAJI.getOrDefault(one, one);
  }

  private static boolean isConsonant(char c) {
    return "aiueo".indexOf(c) < 0 && c >= 'a' && c <= 'z';
  }

  private static Map<String, String> romajiTable() {
    Map<String, String> map = new LinkedHashMap<>();
    put(map, "きゃ", "kya", "きゅ", "kyu", "きょ", "kyo");
    put(map, "しゃ", "sha", "しゅ", "shu", "しょ", "sho");
    put(map, "ちゃ", "cha", "ちゅ", "chu", "ちょ", "cho");
    put(map, "にゃ", "nya", "にゅ", "nyu", "にょ", "nyo");
    put(map, "ひゃ", "hya", "ひゅ", "hyu", "ひょ", "hyo");
    put(map, "みゃ", "mya", "みゅ", "myu", "みょ", "myo");
    put(map, "りゃ", "rya", "りゅ", "ryu", "りょ", "ryo");
    put(map, "ぎゃ", "gya", "ぎゅ", "gyu", "ぎょ", "gyo");
    put(map, "じゃ", "ja", "じゅ", "ju", "じょ", "jo");
    put(map, "びゃ", "bya", "びゅ", "byu", "びょ", "byo");
    put(map, "ぴゃ", "pya", "ぴゅ", "pyu", "ぴょ", "pyo");
    put(map, "ふぁ", "fa", "ふぃ", "fi", "ふぇ", "fe", "ふぉ", "fo");
    put(map, "てぃ", "ti", "でぃ", "di", "とぅ", "tu", "どぅ", "du");
    put(map, "うぃ", "wi", "うぇ", "we", "うぉ", "wo");
    put(map, "ゔぁ", "va", "ゔぃ", "vi", "ゔぇ", "ve", "ゔぉ", "vo");
    map.put("あ", "a");
    map.put("い", "i");
    map.put("う", "u");
    map.put("え", "e");
    map.put("お", "o");
    map.put("か", "ka");
    map.put("き", "ki");
    map.put("く", "ku");
    map.put("け", "ke");
    map.put("こ", "ko");
    map.put("さ", "sa");
    map.put("し", "shi");
    map.put("す", "su");
    map.put("せ", "se");
    map.put("そ", "so");
    map.put("た", "ta");
    map.put("ち", "chi");
    map.put("つ", "tsu");
    map.put("て", "te");
    map.put("と", "to");
    map.put("な", "na");
    map.put("に", "ni");
    map.put("ぬ", "nu");
    map.put("ね", "ne");
    map.put("の", "no");
    map.put("は", "ha");
    map.put("ひ", "hi");
    map.put("ふ", "fu");
    map.put("へ", "he");
    map.put("ほ", "ho");
    map.put("ま", "ma");
    map.put("み", "mi");
    map.put("む", "mu");
    map.put("め", "me");
    map.put("も", "mo");
    map.put("や", "ya");
    map.put("ゆ", "yu");
    map.put("よ", "yo");
    map.put("ら", "ra");
    map.put("り", "ri");
    map.put("る", "ru");
    map.put("れ", "re");
    map.put("ろ", "ro");
    map.put("わ", "wa");
    map.put("ゐ", "wi");
    map.put("ゑ", "we");
    map.put("を", "wo");
    map.put("ん", "n");
    map.put("が", "ga");
    map.put("ぎ", "gi");
    map.put("ぐ", "gu");
    map.put("げ", "ge");
    map.put("ご", "go");
    map.put("ざ", "za");
    map.put("じ", "ji");
    map.put("ず", "zu");
    map.put("ぜ", "ze");
    map.put("ぞ", "zo");
    map.put("だ", "da");
    map.put("ぢ", "ji");
    map.put("づ", "zu");
    map.put("で", "de");
    map.put("ど", "do");
    map.put("ば", "ba");
    map.put("び", "bi");
    map.put("ぶ", "bu");
    map.put("べ", "be");
    map.put("ぼ", "bo");
    map.put("ぱ", "pa");
    map.put("ぴ", "pi");
    map.put("ぷ", "pu");
    map.put("ぺ", "pe");
    map.put("ぽ", "po");
    map.put("ぁ", "a");
    map.put("ぃ", "i");
    map.put("ぅ", "u");
    map.put("ぇ", "e");
    map.put("ぉ", "o");
    map.put("ゃ", "ya");
    map.put("ゅ", "yu");
    map.put("ょ", "yo");
    map.put("ゎ", "wa");
    map.put("ゔ", "vu");
    return Map.copyOf(map);
  }

  private static void put(Map<String, String> map, String... pairs) {
    for (int i = 0; i < pairs.length; i += 2) {
      map.put(pairs[i], pairs[i + 1]);
    }
  }
}
