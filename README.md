# ARCore Depth Lab - Android (Kotlin & Jetpack Compose)

**Depth Lab** is an interactive Android application demonstrating core geometry-aware algorithms and user experiences from Google's ARCore Depth API.

Rewritten as a modern native Android application using Kotlin, Jetpack Compose, Material Design 3, and real-time depth stream processing.

## Features Ported

1. **Oriented 3D Reticle & Surface Normal Estimation (`OrientedReticle`)**
   - Exact port of `ComputeNormalMapFromDepthWeightedMeanGradient` for real-time surface orientation estimation.
   - Interactive 3D reticle ring with inclination angle and surface distance readouts.
   - Paint splat projectile emitter placing multi-colored splatter decals adhering to 3D surface normals.

2. **Dense Depth Map & Turbo Colormap (`DepthMap`)**
   - Polynomial implementation of Google's **Turbo Colormap**, plus Jet, Viridis, and Grayscale.
   - Interactive inspection probe measuring millimeter depth, confidence, and normal vector at any touch point.
   - Camera/depth transition blend slider and distance range limiters.

3. **Depth Mesh Collider & Physics Simulation (`Collider`)**
   - 3D rigid body physics engine simulating gravity, restitution bouncing, surface friction, and sleeping/anchoring.
   - Projectile shapes (Sphere, Cube, Capsule) with adjustable thrust power.
   - Wireframe depth mesh rendering displaying real-time reconstructed polygonal terrain.

4. **Avatar Locomotion - Robochef (`AvatarLocomotion`)**
   - 3D perspective rendered Robochef avatar with particle jet thruster rings.
   - Terrain navigation adapting elevation smoothly over depth surfaces.
   - Interactive action triggers: Jump, Attack, Fast Attack, Frenzy Spin, Teleport, and Idle.

5. **3D Point Cloud Blender (`PointCloud`)**
   - Interactive 3D orbital point cloud viewer with touch yaw/pitch rotation and zoom.
   - Confidence threshold filtering, point size control, and density subsampling.
   - Colormap switching (Turbo depth vs RGB camera).

6. **Atmospheric AR Fog (`FogEffect`)**
   - Distance-based depth haze simulation with customizable density and distance.
   - Color palettes: Neon Cyan, Mystic Violet, Sci-Fi Emerald, Sunlight Amber, and Dense White.

7. **Depth Occlusion & Shadow Receiver (`DepthOcclusion`)**
   - Geometry-aware occlusion demonstration comparing physical foreground blocking vs unnatural overlay.
   - Contact shadow receiver visualization.

## Architecture

- **Language:** Kotlin 2.1
- **UI Framework:** Jetpack Compose + Material Design 3
- **State Management:** MVVM with Coroutines & StateFlow
- **Graphics:** Jetpack Compose Canvas & 3D projection math
- **Target SDK:** Android 36 (Minimum SDK 26)
