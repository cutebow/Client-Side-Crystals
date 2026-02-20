package me.clientsidecrystals.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAgeAccessor {
    @Accessor("age")
    int clientsidecrystals$getAge();

    @Accessor("age")
    void clientsidecrystals$setAge(int age);
}
