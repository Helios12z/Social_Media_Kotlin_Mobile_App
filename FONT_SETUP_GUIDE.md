# Hướng dẫn cài đặt Font Inter cho ứng dụng

## Tổng quan
Tôi đã thiết lập cấu trúc font Inter cho toàn bộ ứng dụng Android của bạn, giống như font được sử dụng trong ảnh mẫu. Font Inter là một font hiện đại, dễ đọc và được sử dụng rộng rãi trong các ứng dụng mobile.

## Các file đã được tạo:

### 1. Thư mục Font
- `app/src/main/res/font/` - Thư mục chứa các file font
- `inter_font_family.xml` - File định nghĩa font family
- `inter_regular.ttf` - Font Inter Regular (placeholder)
- `inter_medium.ttf` - Font Inter Medium (placeholder)
- `inter_semibold.ttf` - Font Inter SemiBold (placeholder)
- `inter_bold.ttf` - Font Inter Bold (placeholder)

### 2. Cập nhật Themes
- `themes.xml` - Đã thêm font Inter làm font mặc định cho toàn bộ ứng dụng

### 3. Text Styles
- `styles.xml` - Đã thêm các style text với font Inter:
  - `TextAppearance.App.Headline1` - Tiêu đề lớn (32sp, Bold)
  - `TextAppearance.App.Headline2` - Tiêu đề vừa (28sp, SemiBold)
  - `TextAppearance.App.Headline3` - Tiêu đề nhỏ (24sp, SemiBold)
  - `TextAppearance.App.Body1` - Nội dung chính (16sp, Regular)
  - `TextAppearance.App.Body2` - Nội dung phụ (14sp, Regular)
  - `TextAppearance.App.Caption` - Chú thích (12sp, Regular)
  - `TextAppearance.App.Button` - Nút bấm (14sp, Medium)

## Cách tải và cài đặt font Inter thực tế:

### Bước 1: Tải font Inter
1. Truy cập [Google Fonts - Inter](https://fonts.google.com/specimen/Inter)
2. Hoặc truy cập [Inter Font Official](https://rsms.me/inter/)
3. Tải các file font với trọng lượng:
   - Inter-Regular.ttf (400)
   - Inter-Medium.ttf (500)
   - Inter-SemiBold.ttf (600)
   - Inter-Bold.ttf (700)

### Bước 2: Thay thế file placeholder
1. Xóa các file placeholder hiện tại trong `app/src/main/res/font/`
2. Copy các file font đã tải vào thư mục này
3. Đổi tên file theo format:
   - `Inter-Regular.ttf` → `inter_regular.ttf`
   - `Inter-Medium.ttf` → `inter_medium.ttf`
   - `Inter-SemiBold.ttf` → `inter_semibold.ttf`
   - `Inter-Bold.ttf` → `inter_bold.ttf`

### Bước 3: Build lại ứng dụng
```bash
./gradlew clean
./gradlew build
```

## Cách sử dụng trong layout:

### Sử dụng style có sẵn:
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Tiêu đề"
    android:textAppearance="@style/TextAppearance.App.Headline2" />
```

### Sử dụng font trực tiếp:
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Nội dung"
    android:fontFamily="@font/inter_font_family"
    android:textSize="16sp" />
```

### Sử dụng font weight cụ thể:
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Tiêu đề đậm"
    android:fontFamily="@font/inter_bold"
    android:textSize="24sp" />
```

## Lưu ý:
- Font Inter sẽ được áp dụng tự động cho toàn bộ ứng dụng thông qua theme
- Các TextView hiện tại sẽ tự động sử dụng font Inter
- Bạn có thể tùy chỉnh thêm các style text theo nhu cầu
- Font Inter hỗ trợ nhiều ngôn ngữ và ký tự đặc biệt

## Kết quả:
Sau khi hoàn thành, ứng dụng của bạn sẽ có font chữ hiện đại, đẹp mắt giống như trong ảnh mẫu, với typography nhất quán trên toàn bộ ứng dụng.