import compose.lens.LambdaLens

package object compose {
  type ~>[-A, +B] = Lambda[A, B]
  type >>-[S, A]  = LambdaLens[S, A]
}
