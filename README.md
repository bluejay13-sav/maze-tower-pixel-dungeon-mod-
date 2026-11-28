# Maze Tower (미궁의 마탑)

**Maze Tower is an unofficial fan mod of [Shattered Pixel Dungeon](https://shatteredpixel.com/shatteredpd/).**
It is not made, affiliated with, or endorsed by Shattered Pixel (Evan Debenham).
Please do not report Maze Tower bugs to the Shattered Pixel Dungeon developers.

Shattered Pixel Dungeon is an open-source roguelike by Evan Debenham, based on the
[source code of Pixel Dungeon](https://github.com/00-Evan/pixel-dungeon-gradle) by [Watabou](https://watabou.itch.io/).
The original game's code is at [00-Evan/shattered-pixel-dungeon](https://github.com/00-Evan/shattered-pixel-dungeon).
If you enjoy it, please consider buying or [supporting](https://www.patreon.com/ShatteredPixel) the original game.

## What is different

You climb a tower instead of descending a dungeon. Maze Tower adds, among other things:
- a mana resource and weapon skills for swords, daggers, bows and the mage's staff
- a four-stage class progression (Sprout → Bloom → Full Bloom → Fruit) with new talents
- elemental reactions (steam, explosion, thaw, freeze) and new status effects
- new bosses, new region art, and two endings

Text is available in Korean and English.

## Test build

The test build is the full game, starting from floor 1. All four classes can be picked
(the Mage's and Rogue's 2nd and 3rd tier classes are not ready yet).
For testing, the in-game menu also has two cheats: **Go to Depth** (go to any floor) and **Get Items** (get any item).

**How to play (Windows):** download the zip from the Releases page, unzip it anywhere, and run `Maze Tower.exe`.
No separate Java install is needed.

The test build is built from the `boss-trial` branch of this repository. The only changes from `main` are
`MazeTower.TEST_BUILD = true` and leaving out the `docs` folder (art drafts and previews).

## Building from source

Follow the original [desktop compile guide](https://github.com/00-Evan/shattered-pixel-dungeon/blob/master/docs/getting-started-desktop.md):
- run: `gradlew desktop:debug`
- runnable jar: `gradlew desktop:release` (output in `desktop/build/libs`)
- Windows app folder with a bundled Java runtime: `gradlew desktop:jpackageImage` (output in `desktop/build/jpackage`)

## License

Maze Tower, like Shattered Pixel Dungeon and Pixel Dungeon, is licensed under the
**GNU General Public License v3.0** — see [LICENSE.txt](LICENSE.txt).
All original credits are kept in the in-game About screen. The full source code of every Maze Tower release is in this repository: https://github.com/bluejay13-sav/maze-tower-pixel-dungeon-mod-

---

# 미궁의 마탑 (한국어)

**미궁의 마탑은 [Shattered Pixel Dungeon](https://shatteredpixel.com/shatteredpd/)(녹픽던)을 바탕으로 만든 비공식 팬 모드입니다.**
원작 개발자(Shattered Pixel, Evan Debenham)가 만들거나 승인한 게임이 아닙니다. 미궁의 마탑의 버그를 원작 개발자에게 알리지 말아 주세요.

## 테스트 버전

1층부터 시작하는 게임 전체입니다. 네 직업을 모두 고를 수 있습니다(마법사·도적의 2차·3차 전직은 아직 준비 중입니다).
시험해 보기 쉽게 게임 메뉴에 치트 두 가지가 있습니다: **층 이동**(원하는 층으로 이동)과 **아이템 꺼내기**(원하는 아이템 받기).

**실행 방법 (Windows):** Releases 페이지에서 zip을 받아 아무 곳에나 압축을 풀고 `Maze Tower.exe`를 실행하세요. 자바를 따로 설치하지 않아도 됩니다.

## 라이선스

원작과 같은 **GNU GPL v3.0** 라이선스를 따릅니다([LICENSE.txt](LICENSE.txt)). 원작 크레딧은 게임 안 크레딧 화면에 그대로 있으며, 배포한 모든 버전의 소스 코드를 이 저장소에 공개합니다.
