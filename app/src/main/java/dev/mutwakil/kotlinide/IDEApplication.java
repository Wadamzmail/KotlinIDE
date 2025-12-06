package dev.mutwakil.kotlinide;

import android.app.Application;
import com.developer.crashx.config.CrashConfig;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;

public class IDEApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    
    CrashConfig.Builder.create()
        .backgroundMode(CrashConfig.BACKGROUND_MODE_SHOW_CUSTOM)
        .enabled(true)
        .showErrorDetails(true)
        .showRestartButton(true)
        .logErrorOnRestart(true)
        .trackActivities(true)
        .apply();

    FileProviderRegistry.getInstance()
        .addFileProvider(new AssetsFileResolver(getApplicationContext().getAssets()));
    try {
      GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    // TODO: Implement this method
  }
}
