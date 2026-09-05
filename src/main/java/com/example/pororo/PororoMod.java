package com.example.pororo;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PororoMod implements ModInitializer {
    private static final Map<UUID, String> PLAYER_LANGUAGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PLAYER_PROGRESS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            CommandManager.literal("pororo")
                .executes(context -> showWelcome(context.getSource()))
                .then(CommandManager.literal("lesson")
                    .executes(context -> startLesson(context.getSource())))
                .then(CommandManager.literal("lang")
                    .then(CommandManager.argument("language", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("ko");
                            builder.suggest("en");
                            builder.suggest("uk");
                            return builder.buildFuture();
                        })
                        .executes(context -> setLanguage(
                            context.getSource(), StringArgumentType.getString(context, "language")))))
        ));
    }

    private static int showWelcome(ServerCommandSource source) {
        source.sendFeedback(() -> localized(source, "pororo.welcome"), false);
        source.sendFeedback(() -> localized(source, "pororo.commands"), false);
        return 1;
    }

    private static int startLesson(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.translatable("pororo.players_only"));
            return 0;
        }

        int nextStep = PLAYER_PROGRESS.merge(player.getUuid(), 1, Integer::sum);
        source.sendFeedback(() -> localized(source, "pororo.lesson.title"), false);
        source.sendFeedback(() -> localized(source, "pororo.lesson.step", nextStep), false);
        source.sendFeedback(() -> localized(source, "pororo.lesson.prompt"), false);
        return nextStep;
    }

    private static int setLanguage(ServerCommandSource source, String language) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.translatable("pororo.players_only"));
            return 0;
        }

        String normalized = language.toLowerCase();
        if (!normalized.equals("ko") && !normalized.equals("en") && !normalized.equals("uk")) {
            source.sendError(Text.translatable("pororo.language.invalid"));
            return 0;
        }

        PLAYER_LANGUAGES.put(player.getUuid(), normalized);
        source.sendFeedback(() -> localized(source, "pororo.language.changed", normalized), false);
        source.sendFeedback(() -> localized(source, "pororo.language.note"), false);
        return 1;
    }

    private static Text localized(ServerCommandSource source, String key, Object... args) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !PLAYER_LANGUAGES.containsKey(player.getUuid())) {
            return Text.translatable(key, args);
        }

        String language = PLAYER_LANGUAGES.get(player.getUuid());
        String message = key;
        if (key.equals("pororo.welcome")) {
            message = language.equals("ko") ? "뽀로로 학습 섬에 오신 것을 환영합니다!"
                : language.equals("uk") ? "Вітаємо на навчальному острові Пороро!"
                : "Pororo Learning Island is ready!";
        } else if (key.equals("pororo.commands")) {
            message = language.equals("ko") ? "/pororo lesson 또는 /pororo lang <ko|en|uk> 를 사용해 보세요."
                : language.equals("uk") ? "Спробуйте /pororo lesson або /pororo lang <ko|en|uk>."
                : "Try /pororo lesson or /pororo lang <ko|en|uk>.";
        } else if (key.equals("pororo.lesson.title")) {
            message = language.equals("ko") ? "1단원: 물고기 세기"
                : language.equals("uk") ? "Урок 1: Порахуємо рибок"
                : "Lesson 1: Count the fish";
        } else if (key.equals("pororo.lesson.step")) {
            message = language.equals("ko") ? "학습 별: " + args[0]
                : language.equals("uk") ? "Навчальні зірки: " + args[0]
                : "Learning stars: " + args[0];
        } else if (key.equals("pororo.lesson.prompt")) {
            message = language.equals("ko") ? "물고기 세 마리를 찾아 선생님께 숫자를 말해 보세요."
                : language.equals("uk") ? "Знайдіть трьох рибок і назвіть число вчителю."
                : "Find three fish, then tell your teacher the number.";
        } else if (key.equals("pororo.language.changed")) {
            message = language.equals("ko") ? "언어 설정 저장됨: " + args[0]
                : language.equals("uk") ? "Мову збережено: " + args[0]
                : "Language preference saved: " + args[0];
        } else if (key.equals("pororo.language.note")) {
            message = language.equals("ko") ? "교실 문장은 선택한 언어로 표시됩니다."
                : language.equals("uk") ? "Текст для класу показується обраною мовою."
                : "Classroom text is now shown in your selected language.";
        }
        return Text.literal(message);
    }
}
