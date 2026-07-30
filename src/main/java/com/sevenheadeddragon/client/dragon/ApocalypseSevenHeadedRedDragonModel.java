package com.sevenheadeddragon.client.dragon;

import com.sevenheadeddragon.entity.dragon.ApocalypseSevenHeadedRedDragonEntity;

/**
 * GeckoLib model for 終末の七つ頭の赤い竜, bound to the supplied
 * {@code dragon.geo.json} / {@code dragon.png} / {@code dragon.animation.json}
 * assets (which contain {@code animation.dragon.attack_bite_1..7},
 * {@code claw}, {@code attack_charge}, {@code attack_tail},
 * {@code fly.start} / {@code fly} / {@code fly.end} and {@code idle}).
 */
public class ApocalypseSevenHeadedRedDragonModel
        extends DragonAssetModel<ApocalypseSevenHeadedRedDragonEntity> {

    public ApocalypseSevenHeadedRedDragonModel() {
        super("dragon");
    }
}
