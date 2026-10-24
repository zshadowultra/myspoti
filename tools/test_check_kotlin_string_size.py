"""Regression fixtures for the source and compiled UTF8 guard."""
import struct
import unittest

from check_kotlin_string_size import byte_sizes, class_utf8_sizes, string_literals


class KotlinStringSizeTest(unittest.TestCase):
    def test_comments_characters_and_raw_strings(self):
        source = '// "ignored"\n/* nested /* "ignored" */ */ val a = "hi\\n"; val c = \'"\'; val r = """raw\\n"""'
        self.assertEqual([value for _, value in string_literals(source)], ["hi\n", "raw\\n"])

    def test_nested_template_quotes_do_not_desynchronize_scanner(self):
        source = 'val a = "${items.joinToString(",") { "${it.name}" }}"; val b = "tail"'
        values = [value for _, value in string_literals(source)]
        self.assertEqual(values, ['${items.joinToString(",") { "${it.name}" }}', "tail"])

    def test_raw_string_closes_at_last_three_quotes(self):
        source = 'val a = """x\\""""; val b = "tail"'
        self.assertEqual([value for _, value in string_literals(source)], ['x\\"', "tail"])

    def test_modified_utf8_accounts_for_nul_and_surrogates(self):
        self.assertEqual(byte_sizes("\0😀"), (5, 8))
        self.assertEqual(byte_sizes("\ud83d\ude00"), (4, 6))
        self.assertEqual(byte_sizes("é"), (2, 2))

    def test_compiled_constant_length_catches_folded_payload(self):
        # The compiler's output contains a single string even when source has chunks.
        payload = b"a" * 60_001
        data = b"\xca\xfe\xba\xbe" + struct.pack(">HHH", 0, 65, 2)
        data += b"\x01" + struct.pack(">H", len(payload)) + payload
        self.assertEqual(list(class_utf8_sizes(data)), [(1, 60_001)])

    def test_double_slot_constants_and_invalid_class(self):
        data = b"\xca\xfe\xba\xbe" + struct.pack(">HHH", 0, 65, 4)
        data += b"\x05" + b"\0" * 8 + b"\x01\0\x02ok"
        self.assertEqual(list(class_utf8_sizes(data)), [(3, 2)])
        with self.assertRaises(ValueError):
            list(class_utf8_sizes(b"invalid"))


if __name__ == "__main__":
    unittest.main()
