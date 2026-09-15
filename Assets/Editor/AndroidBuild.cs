using System;
using System.IO;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEngine;

namespace XuwyxCase.Editor
{
    public static class AndroidBuild
    {
        private const string ScenePath = "Assets/Scenes/MainMenu.unity";
        private const string OutputPath = "build/Android/Xuwyx-Case-2.43.0.apk";

        [MenuItem("Xuwyx Case/Build Android APK")]
        public static void Build()
        {
            ConfigurePlayer();
            Directory.CreateDirectory(Path.GetDirectoryName(OutputPath) ?? "build/Android");

            var options = new BuildPlayerOptions
            {
                scenes = new[] { ScenePath },
                locationPathName = OutputPath,
                target = BuildTarget.Android,
                targetGroup = BuildTargetGroup.Android,
                options = BuildOptions.StrictMode
            };

            var report = BuildPipeline.BuildPlayer(options);
            if (report.summary.result != BuildResult.Succeeded)
            {
                throw new Exception($"Android build failed: {report.summary.result}");
            }

            Debug.Log($"APK created: {OutputPath} ({report.summary.totalSize} bytes)");
        }

        private static void ConfigurePlayer()
        {
            PlayerSettings.companyName = "Xuwyx";
            PlayerSettings.productName = "Xuwyx Case";
            PlayerSettings.bundleVersion = "2.43.0";
            PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "com.xuwyx.app");

            PlayerSettings.defaultInterfaceOrientation = UIOrientation.AutoRotation;
            PlayerSettings.allowedAutorotateToPortrait = false;
            PlayerSettings.allowedAutorotateToPortraitUpsideDown = false;
            PlayerSettings.allowedAutorotateToLandscapeLeft = true;
            PlayerSettings.allowedAutorotateToLandscapeRight = true;

            PlayerSettings.Android.bundleVersionCode = 24300;
            PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel23;
            PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevelAuto;
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARMv7 | AndroidArchitecture.ARM64;
            PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.Mono2x);

            EditorUserBuildSettings.buildAppBundle = false;
            EditorUserBuildSettings.androidBuildSystem = AndroidBuildSystem.Gradle;
        }
    }
}

