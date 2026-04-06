package dev.mutwakil.kotlinide.language.kotlin;

import android.os.Bundle;
import androidx.annotation.NonNull;
// import dev.mutwakil.editor.Editor;
import dev.mutwakil.kotlinide.activity.EditorActivity;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.SymbolPairMatch;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import io.github.rosemoe.sora.text.TextRange;
import io.github.rosemoe.sora.lang.format.AsyncFormatter;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.text.Content;
import androidx.annotation.Nullable;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import dev.mutwakil.kotlinide.language.textmate.EmptyTextMateLanguage;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public class KotlinLanguage extends EmptyTextMateLanguage implements Language {

  private final CodeEditor mEditor;
  private final KotlinAnalyzer mAnalyzer;
  public boolean createIdentifiers = false;
  //  private final TextMateLanguage delegate;
  private static final String GRAMMAR_NAME = "kotlin.tmLanguage";
  private static final String LANGUAGE_PATH = "textmate/kotlin/syntaxes/kotlin.tmLanguage";
  private static final String CONFIG_PATH = "textmate/kotlin/language-configuration.json";
  private static final String SCOPENAME = "source.kotlin";
  private final Formatter formatter =
      new AsyncFormatter() {
        @Nullable
        @Override
        public TextRange formatAsync(@NonNull Content text, @NonNull TextRange cursorRange) {

          return cursorRange;
        }

        @Nullable
        @Override
        public TextRange formatRegionAsync(
            @NonNull Content text,
            @NonNull TextRange rangeToFormat,
            @NonNull TextRange cursorRange) {
          return null;
        }
      };

  @NonNull
  @Override
  public Formatter getFormatter() {
    return formatter;
  }

  public KotlinLanguage(CodeEditor editor) {
    mEditor = editor;
    //  delegate = LanguageManager.createTextMateLanguage(SCOPENAME);
    mAnalyzer = KotlinAnalyzer.create(editor, this);
  }

  @NonNull
  @Override
  public AnalyzeManager getAnalyzeManager() {
    //  return delegate.getAnalyzeManager();
    return mAnalyzer;
  }

  @Override
  public int getInterruptionLevel() {
    return INTERRUPTION_LEVEL_SLIGHT;
  }

    @Override
  public void requireAutoComplete(
      @NonNull ContentReference content,
      @NonNull CharPosition position,
      @NonNull CompletionPublisher publisher,
      @NonNull Bundle extraArguments)
      throws CompletionCancelledException {

    // 1. التحقق من حرف الاستدعاء
    if (position.getIndex() > 0) {
        char c = content.charAt(position.getIndex() - 1);
        if (!isAutoCompleteChar(c)) {
            return;
        }
    }

    // حساب البادئة (Prefix) لمعرفة كم حرفاً سنستبدل من الكلمة الحالية
    final String prefix = CompletionHelper.computePrefix(content, position, this::isAutoCompleteChar);
    final int prefixLength = prefix.length();

    // 2. إعداد المسار (URI)
    String currentFileUri = "file:///storage/emulated/0/.kotlinide/test/com/test/Main.kt";    

    // 3. التحقق من جاهزية الـ LSP Client
    if (dev.mutwakil.kotlinide.lsp.LSPManager.lspClient == null) {
        return;
    }

    try {
        // 4. طلب الإكمال من السيرفر
        CompletableFuture<Either<List<org.eclipse.lsp4j.CompletionItem>, CompletionList>> future = 
            dev.mutwakil.kotlinide.lsp.LSPManager.lspClient.getCompletions(
                currentFileUri, 
                position.line, 
                position.column
            );

        Either<List<org.eclipse.lsp4j.CompletionItem>, CompletionList> result = future.get();
        
        List<org.eclipse.lsp4j.CompletionItem> lspItems;
        if (result.isLeft()) {
            lspItems = result.getLeft();
        } else {
            lspItems = result.getRight().getItems();
        }

        // 5. تحويل نتائج LSP إلى SimpleCompletionItem الخاص بـ Sora
        if (lspItems != null && !lspItems.isEmpty()) {
            
            for (org.eclipse.lsp4j.CompletionItem lspItem : lspItems) {
                if (publisher.isCancelled()) {
                    throw new CompletionCancelledException();
                }

                // تحديد النص الذي سيتم إدراجه (يفضل label إذا كان insertText فارغاً)
                String commitText = lspItem.getInsertText() != null ? lspItem.getInsertText() : lspItem.getLabel();
                String label = lspItem.getLabel();
                String desc = lspItem.getDetail() != null ? lspItem.getDetail() : "Kotlin";

                // استخدام SimpleCompletionItem
                // البارامترات: (العنوان، الوصف، طول البادئة المستبدلة، النص النهائي)
                io.github.rosemoe.sora.lang.completion.SimpleCompletionItem soraItem = 
                    new io.github.rosemoe.sora.lang.completion.SimpleCompletionItem(
                        label, 
                        desc, 
                        prefixLength, 
                        commitText
                    );

                publisher.addItem(soraItem);
            }
        }

    } catch (Exception e) {
        android.util.Log.e("LSP_COMPLETION", "Error fetching completions: " + e.getMessage());
    }
  }



  public boolean isAutoCompleteChar(char p1) {
    return p1 == '.' || MyCharacter.isJavaIdentifierPart(p1);
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
    String text = content.getLine(line).substring(0, column);
    return getIndentAdvance(text);
    // return delegate.getIndentAdvance(content,line,column);
  }

  public int getIndentAdvance(String p1) {
    KotlinLexer lexer = new KotlinLexer(CharStreams.fromString(p1));
    Token token;
    int advance = 0;
    while ((token = lexer.nextToken()) != null) {
      if (token.getType() == KotlinLexer.EOF) {
        break;
      }
      if (token.getType() == KotlinLexer.LCURL) {
        advance++;
        /*case RBRACE:
        advance--;
        break;**/
      }
    }
    advance = Math.max(0, advance);
    return advance * 4;
  }

  @Override
  public boolean useTab() {
    // return delegate.useTab();
    return true;
  }

  @Override
  public SymbolPairMatch getSymbolPairs() {
    // return delegate.getSymbolPairs();
    return new SymbolPairMatch.DefaultSymbolPairs();
  }

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return handlers;
  }

  @Override
  public void destroy() {
    // delegate.destroy();
    mAnalyzer.destroy();
  }

  private final NewlineHandler[] handlers = new NewlineHandler[] {new BraceHandler()};

  class BraceHandler implements NewlineHandler {

    @Override
    public boolean matchesRequirement(
        @NonNull Content text, @NonNull CharPosition position, @Nullable Styles style) {
      int line = position.line;
      if (line < 0 || line >= text.getLineCount()) return false;

      String before = text.subContent(line, 0, line, position.column).toString();
      String after =
          text.subContent(line, position.column, line, text.getLine(line).length()).toString();
      return before.trim().endsWith("{") && after.trim().startsWith("}");
    }

    @Override
    @NonNull
    public NewlineHandleResult handleNewline(
        @NonNull Content text,
        @NonNull CharPosition position,
        @Nullable Styles style,
        int tabSize) {
      int line = position.line;
      String before = text.subContent(line, 0, line, position.column).toString();

      int baseIndent = TextUtils.countLeadingSpaceCount(before, tabSize);
      int bodyIndent = baseIndent + getIndentAdvance(before);
      int closeIndent = baseIndent + getIndentAdvance("");

      String bodyLine = TextUtils.createIndent(bodyIndent, tabSize, false);
      String closeLine = TextUtils.createIndent(closeIndent, tabSize, false);

      StringBuilder sb = new StringBuilder("\n").append(bodyLine).append('\n').append(closeLine);

      int shiftBack = closeLine.length() + 1;
      return new NewlineHandleResult(sb, shiftBack);
    }
  }

  private List<String> listFiles(Path directory, String extension) throws IOException {
    return Files.walk(directory)
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(extension))
        .map(Path::toString)
        .collect(Collectors.toList());
  }
}
