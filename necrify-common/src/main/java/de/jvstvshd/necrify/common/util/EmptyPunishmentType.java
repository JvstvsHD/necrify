package de.jvstvshd.necrify.common.util;

import de.jvstvshd.necrify.api.punishment.PunishmentType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EmptyPunishmentType implements PunishmentType {

    public static final EmptyPunishmentType INSTANCE = new EmptyPunishmentType();

    @Override
    public String getName() {
        return "unknown";
    }

    @Override
    public int getId() {
        return -1;
    }

    @Override
    public boolean isMute() {
        return false;
    }

    @Override
    public boolean isBan() {
        return false;
    }

    @Override
    public @NotNull List<PunishmentType> getRelatedTypes() {
        return List.of();
    }
}
