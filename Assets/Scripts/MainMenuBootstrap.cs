using UnityEngine;

namespace XuwyxCase
{
    public sealed class MainMenuBootstrap : MonoBehaviour
    {
        private const float ReferenceWidth = 1920f;
        private const float ReferenceHeight = 1080f;

        private Texture2D _pixel;
        private GUIStyle _logoStyle;
        private GUIStyle _titleStyle;
        private GUIStyle _menuStyle;
        private GUIStyle _versionStyle;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        private static void CreateMainMenu()
        {
            Screen.autorotateToPortrait = false;
            Screen.autorotateToPortraitUpsideDown = false;
            Screen.autorotateToLandscapeLeft = true;
            Screen.autorotateToLandscapeRight = true;
            Screen.orientation = ScreenOrientation.AutoRotation;

            if (FindObjectOfType<MainMenuBootstrap>() != null)
            {
                return;
            }

            var host = new GameObject("Xuwyx Case Main Menu");
            DontDestroyOnLoad(host);
            host.AddComponent<MainMenuBootstrap>();
        }

        private void Awake()
        {
            Application.targetFrameRate = 60;
            Screen.sleepTimeout = SleepTimeout.NeverSleep;

            _pixel = new Texture2D(1, 1, TextureFormat.RGBA32, false)
            {
                name = "Menu Pixel",
                hideFlags = HideFlags.HideAndDontSave
            };
            _pixel.SetPixel(0, 0, Color.white);
            _pixel.Apply();
        }

        private void OnGUI()
        {
            EnsureStyles();

            var width = Screen.width;
            var height = Screen.height;
            var scale = Mathf.Min(width / ReferenceWidth, height / ReferenceHeight);

            GUI.color = new Color(0.018f, 0.039f, 0.075f, 1f);
            GUI.DrawTexture(new Rect(0f, 0f, width, height), _pixel);

            GUI.color = new Color(0.025f, 0.073f, 0.13f, 1f);
            GUI.DrawTexture(new Rect(0f, height * 0.67f, width, height * 0.33f), _pixel);

            GUI.color = new Color(0.9f, 0.08f, 0.1f, 0.2f);
            GUI.DrawTexture(new Rect(width * 0.24f, height * 0.5f - 3f * scale, width * 0.52f, 6f * scale), _pixel);
            GUI.color = Color.white;

            _logoStyle.fontSize = Mathf.RoundToInt(210f * scale);
            _titleStyle.fontSize = Mathf.RoundToInt(66f * scale);
            _menuStyle.fontSize = Mathf.RoundToInt(25f * scale);
            _versionStyle.fontSize = Mathf.RoundToInt(20f * scale);

            GUI.Label(new Rect(0f, height * 0.19f, width, height * 0.30f), "[E]", _logoStyle);
            GUI.Label(new Rect(0f, height * 0.53f, width, height * 0.12f), "XUWYX CASE", _titleStyle);
            GUI.Label(new Rect(0f, height * 0.66f, width, height * 0.08f), "MAIN MENU", _menuStyle);
            GUI.Label(new Rect(24f * scale, height - 58f * scale, width - 48f * scale, 36f * scale), "VERSION 2.43.0", _versionStyle);
        }

        private void EnsureStyles()
        {
            if (_logoStyle != null)
            {
                return;
            }

            _logoStyle = CreateStyle(new Color(0.95f, 0.06f, 0.08f), FontStyle.Bold, TextAnchor.MiddleCenter);
            _titleStyle = CreateStyle(new Color(0.93f, 0.96f, 1f), FontStyle.Bold, TextAnchor.MiddleCenter);
            _menuStyle = CreateStyle(new Color(0.47f, 0.62f, 0.78f), FontStyle.Normal, TextAnchor.MiddleCenter);
            _versionStyle = CreateStyle(new Color(0.35f, 0.49f, 0.64f), FontStyle.Normal, TextAnchor.LowerRight);
        }

        private static GUIStyle CreateStyle(Color color, FontStyle fontStyle, TextAnchor alignment)
        {
            return new GUIStyle(GUI.skin.label)
            {
                alignment = alignment,
                fontStyle = fontStyle,
                normal = { textColor = color }
            };
        }

        private void OnDestroy()
        {
            if (_pixel != null)
            {
                Destroy(_pixel);
            }
        }
    }
}

