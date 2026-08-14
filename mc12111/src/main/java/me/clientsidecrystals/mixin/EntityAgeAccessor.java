package me.clientsidecrystals.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAgeAccessor {
    @Accessor("age")
    int csc$getAge();
}
// this mod was pain to update ty to the smart ppl who helped