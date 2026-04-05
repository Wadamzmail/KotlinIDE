package dev.mutwakil.kotlinide.language.kotlin;
import dev.mutwakil.kotlinide.analyzer.BaseTextmateAnalyzer;
import dev.mutwakil.kotlinide.language.textmate.EmptyTextMateLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.widget.CodeEditor;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration;

public class KotlinAnalyzer extends BaseTextmateAnalyzer {

   public static String SCOPENAME = "source.kotlin";
 
 public static KotlinAnalyzer create(CodeEditor editor,EmptyTextMateLanguage lang) {
    try {
        return new KotlinAnalyzer(
            editor,
            lang,
             GrammarRegistry.getInstance().findGrammar(SCOPENAME),
            GrammarRegistry.getInstance().findLanguageConfiguration(SCOPENAME),ThemeRegistry.getInstance());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public KotlinAnalyzer(
      CodeEditor editor,
      EmptyTextMateLanguage lang,
      IGrammar grammar,
      LanguageConfiguration languageConfiguration,
      ThemeRegistry theme) throws Exception {
    super(lang,grammar, languageConfiguration, theme);
  }
}
