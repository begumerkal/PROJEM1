package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Blending;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumePower;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tilesize;

public class ImpactReactor extends PowerGenerator{
    protected int timerUse = timers++;

    protected int plasmas = 4;
    protected float warmupSpeed = 0.001f;
    protected float useTime = 60f;
    protected int explosionRadius = 30;
    protected int explosionDamage = 180;

    protected Color plasma1 = Color.valueOf("ffd06b"), plasma2 = Color.valueOf("ff361b");
    protected Color ind1 = Color.valueOf("858585"), ind2 = Color.valueOf("fea080");

    public ImpactReactor(String name){
        super(name);
        hasPower = true;
        hasLiquids = true;
        powerProduction = 2.0f;
        liquidCapacity = 30f;
        hasItems = true;
        outputsPower = consumesPower = true;
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("poweroutput", entity -> new Bar(() ->
            Core.bundle.format("blocks.poweroutput",
            Strings.toFixed(Math.max(entity.tile.block().getPowerProduction(entity.tile) - consumes.get(ConsumePower.class).powerPerTick, 0)*60, 1)),
            () -> Pal.powerBar,
            () -> ((GeneratorEntity)entity).productionEfficiency));
    }

    @Override
    public void update(Tile tile){
        FusionReactorEntity entity = tile.entity();

        if(entity.cons.valid()){
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, warmupSpeed);

            if(entity.timer.get(timerUse, useTime)){
                entity.items.remove(consumes.item(), consumes.itemAmount());
            }
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.01f);
        }

        entity.productionEfficiency = Mathf.pow(entity.warmup, 5f);

        super.update(tile);
    }

    @Override
    public void draw(Tile tile){
        FusionReactorEntity entity = tile.entity();

        Draw.rect(name + "-bottom", tile.drawx(), tile.drawy());

        for(int i = 0; i < plasmas; i++){
            float r = 29f + Mathf.absin(Time.time(), 2f + i * 1f, 5f - i * 0.5f);

            Draw.color(plasma1, plasma2, (float) i / plasmas);
            Draw.alpha((0.3f + Mathf.absin(Time.time(), 2f + i * 2f, 0.3f + i * 0.05f)) * entity.warmup);
            Draw.blend(Blending.additive);
            Draw.rect(name + "-plasma-" + i, tile.drawx(), tile.drawy(), r, r, Time.time() * (12 + i * 6f) * entity.warmup);
            Draw.blend();
        }

        Draw.color();

        Draw.rect(region, tile.drawx(), tile.drawy());

        Draw.rect(name + "-top", tile.drawx(), tile.drawy());

        Draw.color(ind1, ind2, entity.warmup + Mathf.absin(entity.productionEfficiency, 3f, entity.warmup * 0.5f));
        Draw.rect(name + "-light", tile.drawx(), tile.drawy());

        Draw.color();
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-bottom"), Core.atlas.find(name), Core.atlas.find(name + "-top")};
    }

    @Override
    public TileEntity newEntity(){
        return new FusionReactorEntity();
    }

    @Override
    public void onDestroyed(Tile tile){
        super.onDestroyed(tile);

        FusionReactorEntity entity = tile.entity();

        if(entity.warmup < 0.4f) return;

        Effects.shake(6f, 16f, tile.worldx(), tile.worldy());
        Effects.effect(Fx.impactShockwave, tile.worldx(), tile.worldy());
        for(int i = 0; i < 6; i++){
            Time.run(Mathf.random(80), () -> Effects.effect(Fx.impactcloud, tile.worldx(), tile.worldy()));
        }

        Damage.damage(tile.worldx(), tile.worldy(), explosionRadius * tilesize, explosionDamage * 4);


        for(int i = 0; i < 20; i++){
            Time.run(Mathf.random(80), () -> {
                Tmp.v1.rnd(Mathf.random(40f));
                Effects.effect(Fx.explosion, Tmp.v1.x + tile.worldx(), Tmp.v1.y + tile.worldy());
            });
        }

        for(int i = 0; i < 70; i++){
            Time.run(Mathf.random(90), () -> {
                Tmp.v1.rnd(Mathf.random(120f));
                Effects.effect(Fx.impactsmoke, Tmp.v1.x + tile.worldx(), Tmp.v1.y + tile.worldy());
            });
        }
    }

    public static class FusionReactorEntity extends GeneratorEntity{
        public float warmup;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(warmup);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            super.read(stream);
            warmup = stream.readFloat();
        }
    }
}
