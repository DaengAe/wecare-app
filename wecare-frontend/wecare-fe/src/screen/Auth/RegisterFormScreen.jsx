import React, { useState } from "react";
import {
  ScrollView,
  Text,
  View,
  StyleSheet,
  TextInput,
  TouchableOpacity,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useForm, Controller } from "react-hook-form";
import { Theme } from "../../styles/theme";
import { useRoute } from "@react-navigation/native";
import CustomButton from "../../components/buttons/Button";
import { KeyboardAwareScrollView } from "react-native-keyboard-aware-scroll-view";

export default function RegisterFormScreen() {
  const route = useRoute();
  const { isPretender = true } = route.params || {};
  const [selectedGender, setSelectedGender] = useState("");

  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: {
      username: "",
      password: "",
      name: "",
      phoneNumber: "",
      gender: "",
      birthDate: "",
      role: "",
    },
    resolver: yupResolver(schema),
  });

  const onSubmit = (data) => {
    console.log("Form Data:", data);
    // 폼 제출 로직
  };

  // isPretender에 따른 폰트 크기 결정
  const fontSize = isPretender
    ? Theme.FontSize.size_18
    : Theme.FontSize.size_24;
  const lineHeight = isPretender ? Theme.LineHeight[18] : Theme.LineHeight[24];

  return (
    <SafeAreaView style={styles.frameParentLayout}>
      <KeyboardAwareScrollView
        contentContainerStyle={{ flexGrow: 1 }}
        enableOnAndroid={true}
        extraScrollHeight={20} // 선택한 input이 키보드와 겹치지 않도록 여유 공간
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.inputsetParent}>
          {/* 성명 */}
          <View style={styles.inputset}>
            <Text style={[styles.text, { fontSize, lineHeight }]}>성명</Text>
            <Controller
              control={control}
              rules={{ required: "성명을 입력해 주세요." }}
              render={({ field: { onChange, onBlur, value } }) => (
                <TextInput
                  style={[styles.inputsinputtiltle, { fontSize, lineHeight }]}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  value={value}
                  placeholder="성명을 입력해 주세요."
                />
              )}
              name="name"
            />
            {errors.name && (
              <Text style={styles.errorText}>{errors.name.message}</Text>
            )}
          </View>

          {/* 성별 */}
          <View style={styles.radiogender}>
            <Text style={[styles.text, { fontSize, lineHeight }]}>성별</Text>
            <Controller
              control={control}
              rules={{ required: "성별을 선택해 주세요." }}
              render={({ field: { onChange, value } }) => (
                <View style={[styles.radioradiogender, styles.radioFlexBox]}>
                  <TouchableOpacity
                    style={[styles.radio, styles.radioFlexBox]}
                    onPress={() => {
                      onChange("male");
                      setSelectedGender("male");
                    }}
                  >
                    <View
                      style={[
                        styles.buttonsLayout,
                        value === "male" ? styles.buttons : styles.buttons1,
                      ]}
                    >
                      {/* 체크표시 추가 */}
                      {value === "male" && (
                        <View style={styles.checkMark}>
                          <Text style={styles.checkText}>✓</Text>
                        </View>
                      )}
                    </View>
                    <Text style={[styles.textTypo, { fontSize, lineHeight }]}>
                      남성
                    </Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={[styles.radio, styles.radioFlexBox]}
                    onPress={() => {
                      onChange("female");
                      setSelectedGender("female");
                    }}
                  >
                    <View
                      style={[
                        styles.buttonsLayout,
                        value === "female" ? styles.buttons : styles.buttons1,
                      ]}
                    >
                      {/* 체크표시 추가 */}
                      {value === "female" && (
                        <View style={styles.checkMark}>
                          <Text style={styles.checkText}>✓</Text>
                        </View>
                      )}
                    </View>
                    <Text style={[styles.textTypo, { fontSize, lineHeight }]}>
                      여성
                    </Text>
                  </TouchableOpacity>
                </View>
              )}
              name="gender"
            />
            {errors.gender && (
              <Text style={styles.errorText}>{errors.gender.message}</Text>
            )}
          </View>

          {/* 생년월일 */}
          <View style={styles.inputset}>
            <Text style={[styles.text, { fontSize, lineHeight }]}>
              생년월일
            </Text>
            <Controller
              control={control}
              rules={{ required: "생년월일을 입력해 주세요." }}
              render={({ field: { onChange, onBlur, value } }) => (
                <TextInput
                  style={[styles.inputsinputtiltle, { fontSize, lineHeight }]}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  value={value}
                  placeholder="YYYY.MM.DD"
                />
              )}
              name="birthDate"
            />
            {errors.birthDate && (
              <Text style={styles.errorText}>{errors.birthDate.message}</Text>
            )}
          </View>

          {/* 휴대폰 번호 */}
          <View style={styles.inputset}>
            <Text style={[styles.text, { fontSize, lineHeight }]}>
              휴대폰 번호
            </Text>
            <Controller
              control={control}
              rules={{ required: "휴대폰 번호를 입력해 주세요." }}
              render={({ field: { onChange, onBlur, value } }) => (
                <TextInput
                  style={[styles.inputsinputtiltle, { fontSize, lineHeight }]}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  value={value}
                  placeholder="01012345678"
                  keyboardType="numeric"
                />
              )}
              name="phoneNumber"
            />
            {errors.phoneNumber && (
              <Text style={styles.errorText}>{errors.phoneNumber.message}</Text>
            )}
          </View>

          {/* 휴대폰 번호 */}
          <View style={styles.inputset}>
            <Text style={[styles.text, { fontSize, lineHeight }]}>
              휴대폰 번호
            </Text>
            <Controller
              control={control}
              rules={{ required: "휴대폰 번호를 입력해 주세요." }}
              render={({ field: { onChange, onBlur, value } }) => (
                <TextInput
                  style={[styles.inputsinputtiltle, { fontSize, lineHeight }]}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  value={value}
                  placeholder="01012345678"
                  keyboardType="numeric"
                />
              )}
              name="phoneNumber"
            />
            {errors.phoneNumber && (
              <Text style={styles.errorText}>{errors.phoneNumber.message}</Text>
            )}
          </View>

          {/* 아이디 */}
          <View style={styles.inputset}>
            <Text style={[styles.text, { fontSize, lineHeight }]}>아이디</Text>
            <Controller
              control={control}
              rules={{ required: "아이디를 입력해 주세요." }}
              render={({ field: { onChange, onBlur, value } }) => (
                <TextInput
                  style={[styles.inputsinputtiltle, { fontSize, lineHeight }]}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  value={value}
                  placeholder="아이디를 입력해 주세요."
                />
              )}
              name="userId"
            />
            {errors.userId && (
              <Text style={styles.errorText}>{errors.userId.message}</Text>
            )}
          </View>

          {/* 비밀번호 */}
          <View style={styles.inputset}>
            <Text style={[styles.text, { fontSize, lineHeight }]}>
              비밀번호
            </Text>
            <Controller
              control={control}
              rules={{ required: "비밀번호를 입력해 주세요." }}
              render={({ field: { onChange, onBlur, value } }) => (
                <TextInput
                  style={[styles.inputsinputtiltle, { fontSize, lineHeight }]}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  value={value}
                  placeholder="비밀번호를 입력해 주세요."
                  secureTextEntry
                />
              )}
              name="password"
            />
            {errors.password && (
              <Text style={styles.errorText}>{errors.password.message}</Text>
            )}
          </View>

          {/* 제출 버튼 */}
          <View style={styles.buttonWrapper}>
            <CustomButton
              title="회원가입"
              size="large"
              variant="filled"
              isActive={false}
              onPress={handleSubmit(onSubmit)}
            />
          </View>
        </View>
      </KeyboardAwareScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  textTypo1: {
    textAlign: "left",
    fontFamily: Theme.FontFamily.pretendard,
    color: Theme.Colors.customBlack,
  },
  radioFlexBox: {
    flexDirection: "row",
    alignItems: "center",
  },
  buttonsLayout: {
    height: 24,
    borderRadius: 631,
    width: 24,
    justifyContent: "center", // 체크표시 중앙 정렬을 위해 추가
    alignItems: "center", // 체크표시 중앙 정렬을 위해 추가
  },
  frameParentLayout: {
    flex: 1,
    backgroundColor: "#fff",
  },
  textTypo: {
    textAlign: "center",
    fontFamily: Theme.FontFamily.pretendard,
  },
  text: {
    alignSelf: "stretch",
    textAlign: "left",
    fontFamily: Theme.FontFamily.pretendard,
    color: Theme.Colors.customBlack,
  },
  inputsinputtiltle: {
    borderRadius: 10,
    backgroundColor: Theme.Colors.gray1,
    borderColor: Theme.Colors.gray2,
    borderWidth: 1,
    paddingLeft: Theme.Padding.lg,
    paddingTop: 14,
    paddingBottom: 14,
    borderStyle: "solid",
    width: "100%",
  },
  inputset: {
    gap: Theme.Gap.xs,
    width: "100%",
  },
  buttons: {
    backgroundColor: Theme.Colors.purple500,
    paddingHorizontal: 4,
    paddingVertical: 6,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 50,
  },
  buttons1: {
    backgroundColor: Theme.Colors.customWhite,
    borderColor: Theme.Colors.gray7,
    borderWidth: 1.3,
    overflow: "hidden",
    borderStyle: "solid",
    borderRadius: 50,
  },
  // 체크표시 스타일 추가
  checkMark: {
    position: "absolute",
    top: 2,
    width: "100%",
    height: "150%",
    justifyContent: "center",
    alignItems: "center",
  },
  checkText: {
    color: Theme.Colors.customWhite,
    fontSize: 14,
    fontWeight: "bold",
  },
  radio: {
    gap: Theme.Gap.sm,
    justifyContent: "center",
  },
  inactiveText: {
    color: Theme.Colors.gray9,
  },
  radioradiogender: {
    alignSelf: "stretch",
    gap: 40,
  },
  radiogender: {
    alignSelf: "stretch",
    gap: Theme.Gap.xs,
  },
  inputsetParent: {
    flex: 1,
    paddingHorizontal: Theme.Padding.lg,
    paddingVertical: 40,
    gap: 40,
    justifyContent: "flex-start",
    alignItems: "center",
  },
  errorText: {
    color: Theme.Colors.error,
    fontSize: Theme.FontSize.size_12,
    marginTop: 4,
  },
  submitButton: {
    backgroundColor: Theme.Colors.purple500,
    paddingVertical: 12,
    paddingHorizontal: 30,
    borderRadius: 8,
    marginTop: 20,
    width: "100%",
  },
  submitButtonText: {
    color: Theme.Colors.customWhite,
    textAlign: "center",
    fontSize: Theme.FontSize.size_16,
    fontWeight: "600",
  },
  buttonWrapper: {
    width: "100%",
    margin: 10,
  },
});
